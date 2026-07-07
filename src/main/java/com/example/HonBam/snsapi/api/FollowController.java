package com.example.HonBam.snsapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.response.FollowStatusResponse;
import com.example.HonBam.snsapi.dto.response.FollowUserResponseDTO;
import com.example.HonBam.snsapi.dto.response.UserFollowResponseDTO;
import com.example.HonBam.snsapi.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sns/users")
@Slf4j
public class FollowController {

    private final FollowService followService;

    // 유저 프로필 정보
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserFollowResponseDTO> getSnsProfile(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable(value = "userId") String targetUser
    ) {
        UserFollowResponseDTO dto = followService.getSnsProfile(userInfo.getUserId(), targetUser);
        return ResponseEntity.ok(dto);
    }

    // 팔로우 등록
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowStatusResponse> follow(
            @PathVariable String targetId,
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        followService.follow(userInfo.getUserId(), targetId);
        long followerCount = followService.getFollowerCount(targetId);

        return ResponseEntity.ok(new FollowStatusResponse(true, followerCount));
    }

    // 팔로우 취소
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<FollowStatusResponse> unFollow(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String targetId
    ) {
        followService.unFollow(userInfo.getUserId(), targetId);
        long followerCount = followService.getFollowerCount(targetId);

        return ResponseEntity.ok(new FollowStatusResponse(false, followerCount));
    }

    // 팔로우 여부 확인
    @GetMapping("/{targetId}/follow")
    public ResponseEntity<FollowStatusResponse> isFollowing(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String targetId
    ) {
        boolean following = followService.isFollowing(userInfo.getUserId(), targetId);
        long followerCount = followService.getFollowerCount(targetId);

        return ResponseEntity.ok(new FollowStatusResponse(following, followerCount));
    }

    // 팔로워 목록
    @GetMapping("/{targetId}/followers")
    public ResponseEntity<List<FollowUserResponseDTO>> getFollowers(
            @PathVariable String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(followService.getFollowers(targetId, page, size));
    }

    // 팔로잉 목록
    @GetMapping("/{targetId}/following")
    public ResponseEntity<List<FollowUserResponseDTO>> getFollowing(
            @PathVariable String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(followService.getFollowing(targetId, page, size));
    }
}
