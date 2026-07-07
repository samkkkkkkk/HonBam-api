package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.notification.event.FollowerCreatedEvent;
import com.example.HonBam.snsapi.dto.response.FollowUserResponseDTO;
import com.example.HonBam.snsapi.dto.response.UserFollowResponseDTO;
import com.example.HonBam.snsapi.entity.Follow;
import com.example.HonBam.snsapi.entity.FollowId;
import com.example.HonBam.snsapi.repository.FollowRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorProfileResolver authorProfileResolver;

    // 팔로우 등록 (중복/동시 요청 멱등 처리)
    @Transactional
    public void follow(String userId, String targetId) {
        if (userId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }
        if (!userRepository.existsById(targetId)) {
            throw new UserNotFoundException("팔로우할 사용자를 찾을 수 없습니다.");
        }

        int inserted = followRepository.insertIgnore(userId, targetId, LocalDateTime.now());
        if (inserted == 0) {
            return; // 이미 팔로우 상태
        }

        // 비동기 알림 이벤트 발행 (신규 팔로우일 때만)
        eventPublisher.publishEvent(new FollowerCreatedEvent(userId, targetId));
    }

    // 팔로우 취소 (멱등 처리)
    @Transactional
    public void unFollow(String userId, String targetId) {
        followRepository.deleteFollow(userId, targetId);
    }

    // 팔로우 여부 확인
    @Transactional(readOnly = true)
    public boolean isFollowing(String userId, String targetId) {
        return followRepository.existsById(new FollowId(userId, targetId));
    }

    // 팔로워 / 팔로잉 조회
    @Transactional(readOnly = true)
    public long getFollowerCount(String userId) {
        return followRepository.countByIdFollowingId(userId);
    }

    @Transactional(readOnly = true)
    public long getFollowingCount(String userId) {
        return followRepository.countByIdFollowerId(userId);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponseDTO> getFollowers(String userId, int page, int size) {
        List<Follow> follows = followRepository
                .findAllByIdFollowingId(userId, followPageable(page, size))
                .getContent();
        return convertToFollowUserDTOList(follows, f -> f.getId().getFollowerId());
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponseDTO> getFollowing(String userId, int page, int size) {
        List<Follow> follows = followRepository
                .findAllByIdFollowerId(userId, followPageable(page, size))
                .getContent();
        return convertToFollowUserDTOList(follows, f -> f.getId().getFollowingId());
    }

    @Transactional(readOnly = true)
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

        String profileImageUrl = authorProfileResolver.resolve(targetUser);

        return UserFollowResponseDTO.from(targetId, targetUser.getNickname(), profileImageUrl,
                followerCount, followingCount, following, postCount);
    }

    private Pageable followPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // 사용자 정보/프로필 일괄 조회 후 DTO 변환 (건별 개별 조회 N+1 방지)
    private List<FollowUserResponseDTO> convertToFollowUserDTOList(
            List<Follow> follows,
            Function<Follow, String> targetIdExtractor
    ) {
        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> userIds = follows.stream()
                .map(targetIdExtractor)
                .collect(Collectors.toSet());

        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<String, String> profileUrlMap = authorProfileResolver.resolveUrlMap(userIds);

        List<FollowUserResponseDTO> result = new ArrayList<>();
        for (Follow follow : follows) {
            String targetId = targetIdExtractor.apply(follow);
            User user = userMap.get(targetId);
            if (user == null) {
                log.warn("팔로우 상대 사용자를 찾을 수 없습니다. userId: {}", targetId);
                continue;
            }
            result.add(FollowUserResponseDTO.from(user, profileUrlMap.get(targetId), follow.getCreatedAt()));
        }
        return result;
    }
}
