package com.example.HonBam.freeboardapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.freeboardapi.Service.FreeboardService;
import com.example.HonBam.freeboardapi.dto.request.CommentModifyRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardCommentRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardRequestDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardDetailResponseDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardResponseDTO;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
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


        FreeboardResponseDTO responseDTO =
                freeboardService.createContent(requestDTO, userInfo);

        return ResponseEntity.ok().body(responseDTO);

    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<?> contentList(
            @AuthenticationPrincipal TokenUserInfo userInfo
        ) {
        log.info("유저=={}", userInfo);
        if(userInfo == null) {
            return ResponseEntity.badRequest().body("목록을 불러오지 못했습니다.");
        }
        FreeboardResponseDTO responseDTO = freeboardService.retrieve();
        return ResponseEntity.ok().body(responseDTO);

    }

    // 게시글 삭제 요청
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable("id") Long id
    ){
        if(id == null || Long.toString(id).isEmpty()){
            ResponseEntity.badRequest()
                    .body("에러");
        }

        try{
            FreeboardResponseDTO responseDTO = freeboardService.delete(userInfo.getUserId(), id);
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e){
            return ResponseEntity
                    .internalServerError()
                    .body(FreeboardResponseDTO.builder().build());
        }
    }

    // 게시글 수정하기
    @PutMapping("/{id}")
    public ResponseEntity<?> modifyContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable("id") Long id,
            @RequestBody FreeboardRequestDTO RequestDTO
    ) {
        FreeboardDetailResponseDTO modifyContent = freeboardService.modify(userInfo, id, RequestDTO);
        return ResponseEntity.ok().body(modifyContent);

    }

    // 게시글 상세보기
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detailContent(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable("id") Long id
    ){

        return ResponseEntity
                .ok()
                .body(freeboardService.getContent(id));

    }

    // 댓글 등록
    @PostMapping("/comment")
    public ResponseEntity<List<FreeboardComment>> createComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody FreeboardCommentRequestDTO dto
    ){

        return ResponseEntity.ok().body(freeboardService.commentRegist(dto, userInfo));
    }

    // 댓글 목록 요청
    @GetMapping("/comment")
    public ResponseEntity<?> commentLis (
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam Long id
    ){

        List<FreeboardComment> comments = freeboardService.commentList(id);
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
    @PutMapping("/comment")
    public ResponseEntity<?> modifyComment(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody CommentModifyRequestDTO requestDTO
    ){
        if(!freeboardService.validateWriter(userInfo, requestDTO.getId())){
            return ResponseEntity.badRequest().body("fail");
        }
        return ResponseEntity.ok().body(freeboardService.modify(requestDTO));

    }



}
