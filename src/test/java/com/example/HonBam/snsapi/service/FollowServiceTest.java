package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.notification.event.FollowerCreatedEvent;
import com.example.HonBam.snsapi.repository.FollowRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.userapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock AuthorProfileResolver authorProfileResolver;

    @InjectMocks FollowService followService;

    @Test
    @DisplayName("신규 팔로우 → 알림 이벤트 1회 발행")
    void newFollowPublishesEvent() {
        given(userRepository.existsById("target-1")).willReturn(true);
        given(followRepository.insertIgnore(eq("user-1"), eq("target-1"), any(LocalDateTime.class)))
                .willReturn(1);

        followService.follow("user-1", "target-1");

        then(eventPublisher).should().publishEvent(any(FollowerCreatedEvent.class));
    }

    @Test
    @DisplayName("이미 팔로우 중 → 알림 이벤트 발행 안 함 (중복 알림 방지)")
    void duplicateFollowDoesNotPublishEvent() {
        given(userRepository.existsById("target-1")).willReturn(true);
        given(followRepository.insertIgnore(eq("user-1"), eq("target-1"), any(LocalDateTime.class)))
                .willReturn(0);

        followService.follow("user-1", "target-1");

        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("자기 자신 팔로우 → IllegalArgumentException")
    void selfFollowThrows() {
        assertThatThrownBy(() -> followService.follow("user-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);

        then(followRepository).should(never()).insertIgnore(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 팔로우 → UserNotFoundException")
    void followNonexistentUserThrows() {
        given(userRepository.existsById("ghost")).willReturn(false);

        assertThatThrownBy(() -> followService.follow("user-1", "ghost"))
                .isInstanceOf(UserNotFoundException.class);

        then(followRepository).should(never()).insertIgnore(anyString(), anyString(), any());
        then(eventPublisher).should(never()).publishEvent(any());
    }
}
