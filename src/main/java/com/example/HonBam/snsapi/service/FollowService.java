package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.notification.event.FollowerCreatedEvent;
import com.example.HonBam.snsapi.dto.response.UserFollowResponseDTO;
import com.example.HonBam.snsapi.entity.Follow;
import com.example.HonBam.snsapi.entity.FollowId;
import com.example.HonBam.snsapi.repository.FollowRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.PostUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostUtils postUtils;
    private final ApplicationEventPublisher eventPublisher;

    // 팔로우 등록
    public void follow(String userId, String targetId) {
        if (userId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }

        FollowId id = new FollowId(userId, targetId);
        if (!followRepository.existsById(id)) {
            followRepository.save(new Follow(id, LocalDateTime.now()));
        }

        // 비동기 알림 에븐트 발행
        eventPublisher.publishEvent(new FollowerCreatedEvent(userId, targetId));

    }


    // 팔로우 취소
    public void unFollow(String userId, String targetId) {
        FollowId id = new FollowId(userId, targetId);
        if (!followRepository.existsById(id)) return;
        followRepository.deleteById(id);
    }

    // 팔로우 여부 확인
    public boolean isFollowing(String userId, String targetId) {
        return followRepository.existsById(new FollowId(userId, targetId));
    }

    // 팔로워 / 팔로잉 조회
    public long getFollowerCount(String userId) {
        return followRepository.countByIdFollowingId(userId);
    }

    public long getFollowingCount(String userId) {
        return followRepository.countByIdFollowerId(userId);
    }

    public List<Follow> getFollowers(String userId) {
        return followRepository.findAllByIdFollowingId(userId);
    }

    public List<Follow> getFollowing(String userId) {
        return followRepository.findAllByIdFollowerId(userId);
    }

    public UserFollowResponseDTO getSnsProfile(String viewerId, String targetId) {
        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        long followerCount = getFollowerCount(targetId);
        long followingCount = getFollowingCount(targetId);

        long postCount = postRepository.countByAuthorId(targetId);

        boolean following = false;

        if (viewerId != null && !viewerId.equals(targetId)) {
            following = followRepository.existsById(new FollowId(viewerId, targetId));
        }

        String profileImageUrl = postUtils.buildProfileUrl(targetUser);

        return UserFollowResponseDTO.from(targetId, targetUser.getNickname(), profileImageUrl, followerCount, followingCount, following, postCount);

    }
}
