package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.notification.event.LikeCreateEvent;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostRepository postRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks LikeService likeService;

    private final Post post = Post.builder().id(1L).authorId("author-1").content("내용").build();

    @Test
    @DisplayName("신규 좋아요 → 카운트 증가 + 알림 이벤트 발행")
    void newLikeIncreasesCountAndPublishesEvent() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.insertIgnore(eq("user-1"), eq(1L), any(LocalDateTime.class)))
                .willReturn(1);

        likeService.addLike("user-1", 1L);

        then(postRepository).should().increaseLikeCount(1L);
        then(eventPublisher).should().publishEvent(any(LikeCreateEvent.class));
    }

    @Test
    @DisplayName("중복 좋아요 → 카운트 미증가 + 이벤트 미발행 (멱등)")
    void duplicateLikeIsIdempotent() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.insertIgnore(eq("user-1"), eq(1L), any(LocalDateTime.class)))
                .willReturn(0);

        likeService.addLike("user-1", 1L);

        then(postRepository).should(never()).increaseLikeCount(anyLong());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시물 좋아요 → PostNotFoundException")
    void likeNonexistentPostThrows() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.addLike("user-1", 99L))
                .isInstanceOf(PostNotFoundException.class);

        then(postLikeRepository).should(never()).insertIgnore(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("좋아요 취소 → 삭제 성공 시에만 카운트 감소")
    void removeLikeDecreasesCount() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.deleteLike("user-1", 1L)).willReturn(1);

        likeService.removeLike("user-1", 1L);

        then(postRepository).should().decreaseLikeCount(1L);
    }

    @Test
    @DisplayName("좋아요 상태가 아닌데 취소 → 카운트 미감소 (멱등)")
    void removeLikeWithoutExistingLikeIsIdempotent() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.deleteLike("user-1", 1L)).willReturn(0);

        likeService.removeLike("user-1", 1L);

        then(postRepository).should(never()).decreaseLikeCount(anyLong());
    }
}
