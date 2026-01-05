package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.CommentUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sns/posts/{postId}/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment API", description = "게시글 댓글 관련 API")
public class SnsCommentController {

    private final CommentService commentService;

    // 댓글 등록
    @Operation(summary = "댓글 등록", description = "특정 게시글에 댓글을 등록합니다.")
    @PostMapping
    public ResponseEntity<CommentResponseDTO> createComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @RequestBody CommentCreateRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(commentService.createComment(userInfo.getUserId(), postId, requestDTO));
    }

    // 댓글 수정
    @Operation(summary = "댓글 수정", description = "댓글 작성자만 자신의 댓글을 수정할 수 있습니다.")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(commentService.updateComment(userInfo.getUserId(), commentId, requestDTO));
    }

    // 댓글 삭제
    @Operation(summary = "댓글 삭제", description = "댓글 작성자만 자신의 댓글을 삭제할 수 있습니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userInfo.getUserId(), commentId);
        return ResponseEntity.ok("댓글이 삭제되었습니다.");
    }

    // 댓글 목록 조회
    @Operation(summary = "댓글 목록 조회", description = "특정 게시물의 모든 댓글을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    // 대댓글 목록 조회
    @Operation(summary = "대댓글 목록 조회", description = "특정 댓글의 대댓글 목록을 조회합니다.")
    @GetMapping("/replies/{parentId}")
    public ResponseEntity<List<CommentResponseDTO>> getReplies(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @PathVariable Long parentId
    ) {
        return ResponseEntity.ok(commentService.getReplies(postId, parentId));
    }

}
