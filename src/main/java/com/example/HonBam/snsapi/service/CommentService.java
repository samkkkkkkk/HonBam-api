package com.example.HonBam.snsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.CommentUpdateRequestDTO;
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

    // 댓글 작성
    @Transactional
    public CommentResponseDTO createComment(String userId, Long postId, CommentCreateRequestDTO requestDTO) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(userId)
                .content(requestDTO.getContent())
                .parentId(requestDTO.getParentId())
                .build();

        Comment saved = commentRepository.save(comment);

        // 게시글 댓글 수 증가
        post.increaseCommentCount();
        return CommentResponseDTO.from(saved);
    }

    // 댓글 수정
    @Transactional
    public CommentResponseDTO updateComment(String userId, Long commentId, CommentUpdateRequestDTO requestDTO) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 수정할 수 있습니다.");
        }

        comment.editContent(requestDTO.getContent());
        return CommentResponseDTO.from(comment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(String userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다ㅏ."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException("본인 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);

        postRepository.findById(comment.getPostId())
                .ifPresent(Post::decreaseCommentCount);

    }

    // 특정 게시글의 모든 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 특정 댓글의 대댓글 목록
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getReplies(Long postId, Long parentId) {
        return commentRepository.findByPostIdAndParentIdOrderByCreatedAt(postId, parentId)
                .stream()
                .map(CommentResponseDTO::from)
                .collect(Collectors.toList());
    }
}
