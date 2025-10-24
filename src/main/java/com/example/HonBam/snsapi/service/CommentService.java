package com.example.HonBam.snsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.entity.Comment;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.repository.CommentRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TokenUserInfo)) {
            throw new SecurityException("인증되지 않은 사용자입니다.");
        }
        return ((TokenUserInfo) auth.getPrincipal()).getUserId();
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponseDTO createComment(Long postId, CommentCreateRequestDTO request) {
        String userId = currentUserId();

        // 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(userId)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);

        // 게시글 댓글 수 +1
        post.increaseCommentCount();

        return toResponse(saved);
    }

    /**
     * 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByIdAsc(postId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponseDTO updateComment(Long commentId, CommentCreateRequestDTO request) {
        String userId = currentUserId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 수정할 수 있습니다.");
        }

        comment.editContent(request.getContent());
        // updatedAt은 @UpdateTimestamp로 자동 갱신됨

        return toResponse(comment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId) {
        String userId = currentUserId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);

        // 댓글 수 감소
        postRepository.findById(comment.getPostId())
                .ifPresent(Post::decreaseCommentCount);
    }

    private CommentResponseDTO toResponse(Comment c) {
        return CommentResponseDTO.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .authorId(c.getAuthorId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
