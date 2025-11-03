package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sns/users")
@Slf4j
public class FollowController {

    private final FollowService followService;

    // 팔로우 등록
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<?> follow(
            @PathVariable String targetId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {

        followService.follow(userInfo.getUserId(), targetId);
        long followerCount = followService.getFollowerCount(targetId);

        return ResponseEntity.ok(Map.of(
                "following", true,
                "followerCount", followerCount
        ));
    }

    // 팔로우 취소
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<?> unFollow(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String targetId
    ) {
        followService.unFollow(userInfo.getUserId(), targetId);
        long followerCount = followService.getFollowerCount(targetId);

        return ResponseEntity.ok(Map.of(
                "following", false,
                "followerCount", followerCount
        ));
    }

    // 팔로우 여부 확인
    @GetMapping("/{targetId}/follow")
    public ResponseEntity<?> isFollowing(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String targetId
    ) {
        boolean following = followService.isFollowing(userInfo.getUserId(), targetId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    // 팔로워 목록
    @GetMapping("/{targetId}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable String targetId) {
        return ResponseEntity.ok(followService.getFollowers(targetId));
    }

    // 팔로윙 목록
    @GetMapping("/{targetId}/following")
    public ResponseEntity<?> getFollowing(@PathVariable String targetId) {
        return ResponseEntity.ok(followService.getFollowing(targetId));
    }
}
