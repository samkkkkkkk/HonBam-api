package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.dto.response.TodayShotResponseDTO;
import com.example.HonBam.snsapi.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/sns/feed")
@Slf4j
public class PostController {

    private final PostService postService;

    // 게시물 등록
    @PostMapping
    public ResponseEntity<PostResponseDTO> createPost(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody PostCreateRequestDTO requestDTO
    ) {
        PostResponseDTO response = postService.createPost(userInfo.getUserId(), requestDTO);
        return ResponseEntity.ok().body(response);
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostDetail(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId) {
        PostResponseDTO dto = postService.getPostDetail(userInfo.getUserId(), postId);
        return ResponseEntity.ok(dto);
    }
    // 내 게시물 조회
    @GetMapping("/my")
    public ResponseEntity<List<PostResponseDTO>> getMyFeed(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<PostResponseDTO> myFeeds = postService.getMyFeeds(userInfo.getUserId(), page, size);
        return ResponseEntity.ok().body(myFeeds);
    }

    // 게시글 feed 가져오기
    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> getFeedPosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(postService.getFeedPosts(userInfo.getUserId(), page, size));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> updatePost(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequestDTO requestDTO
    ) {
        PostResponseDTO response = postService.updatePost(userInfo.getUserId(), postId, requestDTO);
        return ResponseEntity.ok().body(response);
    }


    // 특정 사용자 게시글 조회
    @GetMapping("/user/{authorId}")
    public ResponseEntity<List<PostResponseDTO>> getUserPosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<PostResponseDTO> responseDto = postService.getUserPosts(userInfo.getUserId(), authorId, page, size);
        return ResponseEntity.ok().body(responseDto);
    }

    // 게시글 탐색
    @GetMapping("/explore")
    public ResponseEntity<List<PostResponseDTO>> getExplorePosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = (userInfo != null) ? userInfo.getUserId() : null;
        List<PostResponseDTO> explorePosts = postService.getExplorePosts(userId, sort, page, size);
        return ResponseEntity.ok().body(explorePosts);
    }


    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public void deletePost(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        postService.deletePost(userInfo.getUserId(), postId);
    }

    // 오늘의 인증샷
    @GetMapping("/today-shots")
    public ResponseEntity<List<TodayShotResponseDTO>> getTodayShots(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TodayShotResponseDTO> shots = postService.getTodayShots(limit);
        return ResponseEntity.ok().body(shots);
    }

}
