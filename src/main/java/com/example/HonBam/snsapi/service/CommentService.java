package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.CommentNotFoundException;
import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.CommentUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.entity.Comment;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.repository.CommentRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.userapi.entity.LoginProvider;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public User getAuthor(String authorId) {
        return userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));
    }

    // 댓글 작성
    @Transactional
    public CommentResponseDTO createComment(String userId, Long postId, CommentCreateRequestDTO requestDTO) {

        if (requestDTO.getParentId() != null) {
            Comment parent = commentRepository.findByIdAndPostId(requestDTO.getParentId(), postId)
                    .orElseThrow(() -> new CommentNotFoundException("댓글이 존재하지 않습니다."));

            if (parent.getParentId() != null) {
                throw new RuntimeException("대댓글에 댓글을 작성할 수 없습니다.");
            }
        }

        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("게시글이 존재하지 않습니다."));

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
        int updated = postRepository.increaseCommentCount(postId);
        if (updated == 0) {
            throw new IllegalArgumentException("댓글 증가 처리에 실패했습니다.");
        }

        return convertToCommentDTO(saved);
    }

    // 댓글 수정
    @Transactional
    public CommentResponseDTO updateComment(String userId, Long commentId, CommentUpdateRequestDTO requestDTO) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 수정할 수 있습니다.");
        }

        comment.editContent(requestDTO.getContent());
        return convertToCommentDTO(comment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(String userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);

        int updated = postRepository.decreaseCommentCount(comment.getPostId());
        if (updated == 0) {
            throw new IllegalArgumentException("댓글 삭제는 완료 되었지만 댓글 수 감소 처리에 실패했습니다.");
        }

    }

    // 특정 댓글의 대댓글 목록
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getReplies(Long postId, Long parentId) {
        return commentRepository.findByPostIdAndParentIdOrderByCreatedAt(postId, parentId)
                .stream()
                .map(comment -> convertToCommentDTO(comment))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<Long, CommentResponseDTO> map = new HashMap<>();
        List<CommentResponseDTO> roots = new ArrayList<>();

        for (Comment c : comments) {
            CommentResponseDTO dto = convertToCommentDTO(c);
            map.put(c.getId(), dto);
            if (c.getParentId() == null) {
                roots.add(dto);
            }
        }

        for (Comment c : comments) {
            if (c.getParentId() != null) {
                CommentResponseDTO parent = map.get(c.getParentId());
                CommentResponseDTO child = map.get(c.getId());
                if (parent != null && child != null) {
                    parent.getChildren().add(child);
                }
            }
        }

        return roots;
    }

    private String buildProfileUrl(User author) {
        if (author.getLoginProvider() != LoginProvider.LOCAL) {
            return author.getProfileImg();
        }

        if (author.getProfileImg() == null) {
            return "default-profile.png";
        }

        return "uploads/" + author.getProfileImg();
    }

    private CommentResponseDTO convertToCommentDTO(Comment comment) {
        User author = userRepository.findById(comment.getAuthorId())
                .orElseThrow(() -> new UserNotFoundException("댓글 작성자를 찾을 수 없습니다."));

        String authorNickname = author.getNickname();
        String profileUrl = buildProfileUrl(author);

        return CommentResponseDTO.from(comment, authorNickname, profileUrl);
    }

}
