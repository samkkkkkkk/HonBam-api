package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.service.LikeService;
import com.example.HonBam.snsapi.service.SnsService;
import lombok.RequiredArgsConstructor;
import org.hibernate.hql.internal.ast.tree.ResolvableNode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sns/posts")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    // 좋아요 등록
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> addLike(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        likeService.addLike(userInfo.getUserId(), postId);
        long likeCount = likeService.getLikeCount(postId);

        return ResponseEntity.ok(Map.of(
                "liked", true,
                "likeCount", likeCount
        ));
    }

    // 좋아요 취소
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<?> removeLike(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        likeService.removeLike(userInfo.getUserId(), postId);
        long likeCount = likeService.getLikeCount(postId);

        return ResponseEntity.ok(Map.of(
                "liked", false,
                "likeCount", likeCount
        ));
    }

    // 좋아요 여부 조회
    @GetMapping("/{postId}/like")
    public ResponseEntity<?> checkLiked(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable Long postId
    ) {
        boolean liked = likeService.isLiked(userInfo.getUserId(), postId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // 좋아요 수 조회
    @GetMapping("/{postId}/like-count")
    public ResponseEntity<?> getLikeCount(@PathVariable Long postId) {
        return ResponseEntity.ok(Map.of(
                "likeCount", likeService.getLikeCount(postId)
        ));
    }


}
