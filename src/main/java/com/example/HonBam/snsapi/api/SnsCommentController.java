package com.example.HonBam.snsapi.api;

import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Slf4j
@RequiredArgsConstructor
public class SnsCommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성
     */
//    @PostMapping("/posts/{postId}/comments")
//    public CommentResponseDTO createComment(
//            @PathVariable Long postId,
//            @RequestBody CommentCreateRequestDTO request
//    ) {
//        log.info("[POST] create comment for post {}", postId);
//        return commentService.createComment(postId, request);
//    }
    @PostMapping("/posts/{postId}/comments")
    public CommentResponseDTO createComment(
            @PathVariable Long postId,
            @RequestBody CommentCreateRequestDTO requestDTO
    ) {
        return commentService.createComment(postId, requestDTO);
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponseDTO> getComments(@PathVariable Long postId) {

        log.info("[GET] list comments for post {}", postId);
        return commentService.getComments(postId);
    }

    /**
     * 댓글 수정
     */
    @PatchMapping("/comments/{commentId}")
    public CommentResponseDTO updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentCreateRequestDTO request
    ) {
        log.info("[PATCH] update comment {}", commentId);
        return commentService.updateComment(commentId, request);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId) {
        log.info("[DELETE] delete comment {}", commentId);
        commentService.deleteComment(commentId);
    }}
