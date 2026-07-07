package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.CommentNotFoundException;
import com.example.HonBam.exception.InvalidCommentException;
import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.SnsAccessDeniedException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.CommentUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.entity.Comment;
import com.example.HonBam.snsapi.repository.CommentRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuthorProfileResolver authorProfileResolver;

    // 댓글 작성
    @Transactional
    public CommentResponseDTO createComment(String userId, Long postId, CommentCreateRequestDTO requestDTO) {

        if (requestDTO.getParentId() != null) {
            Comment parent = commentRepository.findByIdAndPostId(requestDTO.getParentId(), postId)
                    .orElseThrow(() -> new CommentNotFoundException("댓글이 존재하지 않습니다."));

            if (parent.getParentId() != null) {
                throw new InvalidCommentException("대댓글에 댓글을 작성할 수 없습니다.");
            }
            if (parent.isDeleted()) {
                throw new InvalidCommentException("삭제된 댓글에는 답글을 작성할 수 없습니다.");
            }
        }

        postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(userId)
                .content(requestDTO.getContent())
                .parentId(requestDTO.getParentId())
                .build();

        Comment saved = commentRepository.save(comment);

        // 게시글 댓글 수 증가
        postRepository.increaseCommentCount(postId);

        return convertToCommentDTO(saved, user.getNickname(), authorProfileResolver.resolve(user));
    }

    // 댓글 수정
    @Transactional
    public CommentResponseDTO updateComment(String userId, Long postId, Long commentId, CommentUpdateRequestDTO requestDTO) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new CommentNotFoundException("댓글이 존재하지 않습니다."));

        if (comment.isDeleted()) {
            throw new InvalidCommentException("삭제된 댓글은 수정할 수 없습니다.");
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new SnsAccessDeniedException("본인 댓글만 수정할 수 있습니다.");
        }

        comment.editContent(requestDTO.getContent());

        User author = userRepository.findById(comment.getAuthorId())
                .orElseThrow(() -> new UserNotFoundException("댓글 작성자를 찾을 수 없습니다."));

        return convertToCommentDTO(comment, author.getNickname(), authorProfileResolver.resolve(author));
    }

    // 댓글 삭제 — 대댓글이 있으면 soft delete, 없으면 hard delete
    @Transactional
    public void deleteComment(String userId, Long postId, Long commentId) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        if (comment.isDeleted()) {
            throw new CommentNotFoundException("이미 삭제된 댓글입니다.");
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new SnsAccessDeniedException("본인 댓글만 삭제할 수 있습니다.");
        }

        if (commentRepository.existsByParentId(comment.getId())) {
            comment.markDeleted();
        } else {
            commentRepository.delete(comment);
        }

        postRepository.decreaseCommentCount(comment.getPostId());
    }

    // 특정 댓글의 대댓글 목록
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getReplies(Long postId, Long parentId) {
        return convertToCommentDTOList(commentRepository.findByPostIdAndParentIdOrderByCreatedAt(postId, parentId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<CommentResponseDTO> dtos = convertToCommentDTOList(comments);

        Map<Long, CommentResponseDTO> map = new HashMap<>();
        List<CommentResponseDTO> roots = new ArrayList<>();

        for (CommentResponseDTO dto : dtos) {
            map.put(dto.getId(), dto);
            if (dto.getParentId() == null) {
                roots.add(dto);
            }
        }

        for (CommentResponseDTO dto : dtos) {
            if (dto.getParentId() != null) {
                CommentResponseDTO parent = map.get(dto.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }

        return roots;
    }

    // 작성자/프로필 일괄 조회 후 DTO 변환 (댓글별 개별 조회 N+1 방지)
    private List<CommentResponseDTO> convertToCommentDTOList(List<Comment> comments) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<String, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<String, String> profileUrlMap = authorProfileResolver.resolveUrlMap(authorIds);

        List<CommentResponseDTO> result = new ArrayList<>();
        for (Comment comment : comments) {
            User author = authorMap.get(comment.getAuthorId());
            if (author == null) {
                log.warn("댓글 작성자를 찾을 수 없습니다. commentId: {}, authorId: {}",
                        comment.getId(), comment.getAuthorId());
            }
            String nickname = (author != null) ? author.getNickname() : null;
            String profileUrl = profileUrlMap.get(comment.getAuthorId());
            result.add(convertToCommentDTO(comment, nickname, profileUrl));
        }
        return result;
    }

    private CommentResponseDTO convertToCommentDTO(Comment comment, String authorNickname, String profileUrl) {
        CommentResponseDTO dto = CommentResponseDTO.from(comment, authorNickname, profileUrl);
        if (comment.isDeleted()) {
            dto.maskAsDeleted(DELETED_COMMENT_CONTENT);
        }
        return dto;
    }

}
