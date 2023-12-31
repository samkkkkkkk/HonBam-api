package com.example.HonBam.freeboardapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.freeboardapi.Service.FreeboardService;
import com.example.HonBam.freeboardapi.dto.request.FreeboardRequestDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardDetailResponseDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardResponseDTO;
import com.example.HonBam.freeboardapi.entity.Freeboard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

        FreeboardResponseDTO responseDTO = freeboardService.retrieve(userInfo.getUserId());
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



}
