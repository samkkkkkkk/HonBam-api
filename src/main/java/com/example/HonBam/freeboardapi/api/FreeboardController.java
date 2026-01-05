package com.example.HonBam.freeboardapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.freeboardapi.dto.request.CommentModifyRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardCommentRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardRequestDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardCommentResponseDTO;
import com.example.HonBam.freeboardapi.service.FreeboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/freeboard")
@CrossOrigin
public class FreeboardController {

    private final FreeboardService freeboardService;

    // 게시글 등록 요청
    @PostMapping
    public ResponseEntity<?> createContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody FreeboardRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(freeboardService.createContent(requestDTO, userInfo));
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<?> contentList() {
        return ResponseEntity.ok(freeboardService.retrieve());
    }

    // 게시글 삭제 요청
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable("id") Long id
    ){
        return ResponseEntity.ok(freeboardService.delete(userInfo.getUserId(), id));
    }

    // 게시글 수정하기
    @PatchMapping("/{id}")
    public ResponseEntity<?> modifyContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable("id") Long id,
            @RequestBody FreeboardRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(freeboardService.modify(userInfo, id, requestDTO));

    }

    // 게시글 상세보기
    @GetMapping("/{id}")
    public ResponseEntity<?> detailContent(@PathVariable("id") Long id){
        return ResponseEntity.ok(freeboardService.getContent(id));
    }

    // 댓글 등록
    @PostMapping("/comment")
    public ResponseEntity<List<FreeboardCommentResponseDTO>> createComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody FreeboardCommentRequestDTO dto
    ){

        return ResponseEntity.ok(freeboardService.commentRegist(dto, userInfo));
    }

    // 댓글 목록 요청
    @GetMapping("/comment")
    public ResponseEntity<?> commentLis (
            @RequestParam Long id
    ){

        List<FreeboardCommentResponseDTO> comments = freeboardService.commentList(id);
        return ResponseEntity.ok().body(comments);
    }

    // 댓글 삭제 요청
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<?> deleteComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable() Long commentId
    ) {

        return ResponseEntity.ok().body(freeboardService.commentDelete(userInfo, commentId));

    }

    // 댓글 수정
    @PatchMapping("/comment")
    public ResponseEntity<?> modifyComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody CommentModifyRequestDTO requestDTO
    ){
        return ResponseEntity.ok(freeboardService.modify(requestDTO, userInfo));
    }



}
