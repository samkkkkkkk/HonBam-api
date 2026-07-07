package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.response.LikeCountResponse;
import com.example.HonBam.snsapi.dto.response.LikeStatusResponse;
import com.example.HonBam.snsapi.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sns/posts")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // 좋아요 등록
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeStatusResponse> addLike(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        likeService.addLike(userInfo.getUserId(), postId);
        int likeCount = likeService.getLikeCount(postId);

        return ResponseEntity.ok(new LikeStatusResponse(true, likeCount));
    }

    // 좋아요 취소
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<LikeStatusResponse> removeLike(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        likeService.removeLike(userInfo.getUserId(), postId);
        int likeCount = likeService.getLikeCount(postId);

        return ResponseEntity.ok(new LikeStatusResponse(false, likeCount));
    }

    // 좋아요 여부 조회
    @GetMapping("/{postId}/like")
    public ResponseEntity<LikeStatusResponse> checkLiked(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        boolean liked = likeService.isLiked(userInfo.getUserId(), postId);
        int likeCount = likeService.getLikeCount(postId);

        return ResponseEntity.ok(new LikeStatusResponse(liked, likeCount));
    }

    // 좋아요 수 조회
    @GetMapping("/{postId}/like-count")
    public ResponseEntity<LikeCountResponse> getLikeCount(@PathVariable Long postId) {
        return ResponseEntity.ok(new LikeCountResponse(likeService.getLikeCount(postId)));
    }

}
