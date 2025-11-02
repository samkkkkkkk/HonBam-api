package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
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
    public ResponseEntity<?> createPost(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody PostCreateRequestDTO requestDTO
    ) {
        PostResponseDTO response = postService.createPost(userInfo.getUserId(), requestDTO);
        return ResponseEntity.ok().body(response);
    }

    // 내 게시물 조회
    @GetMapping("/my")
    public ResponseEntity<?> getMyFeed(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<PostResponseDTO> myFeeds = postService.getMyFeeds(userInfo.getUserId(), page, size);
        return ResponseEntity.ok().body(myFeeds);
    }

    // 게시글 feed 가져오기
    @GetMapping
    public ResponseEntity<?> getFeedPosts(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(postService.getFeedPosts(userInfo.getUserId(), page, size));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequestDTO requestDTO
    ) {
        PostResponseDTO response = postService.updatePost(userInfo.getUserId(), postId, requestDTO);
        return ResponseEntity.ok().body(response);
    }


    // 특정 사용자 게시글 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponseDTO>> getUserPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<PostResponseDTO> responseDto = postService.getUserPosts(userId, page, size);
        return ResponseEntity.ok().body(responseDto);
    }

    // 게시글 탐색
    @GetMapping("/explore")
    public ResponseEntity<?> getExplorePosts(
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<PostResponseDTO> explorePosts = postService.getExplorePosts(sort, page, size);
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

}
