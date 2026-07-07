package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.notification.event.LikeCreateEvent;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 좋아요 추가 (중복/동시 요청 멱등 처리)
    @Transactional
    public void addLike(String userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글이 존재하지 않습니다."));

        int inserted = postLikeRepository.insertIgnore(userId, postId, LocalDateTime.now());
        if (inserted == 0) {
            return; // 이미 좋아요 상태
        }

        postRepository.increaseLikeCount(postId);

        // 비동기 알림 이벤트 발행 (신규 좋아요일 때만)
        eventPublisher.publishEvent(new LikeCreateEvent(userId, postId, post.getAuthorId()));
    }

    // 좋아요 취소 (멱등 처리)
    @Transactional
    public void removeLike(String userId, Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글이 존재하지 않습니다."));

        int deleted = postLikeRepository.deleteLike(userId, postId);
        if (deleted == 0) {
            return; // 좋아요 상태가 아님
        }

        postRepository.decreaseLikeCount(postId);
    }

    // 좋아요 여부 확인
    @Transactional(readOnly = true)
    public boolean isLiked(String userId, Long postId) {
        return postLikeRepository.existsById(new PostLikeId(userId, postId));
    }

    // 좋아요 수 조회
    @Transactional(readOnly = true)
    public int getLikeCount(Long postId) {
        Integer count = postRepository.findLikeCount(postId);
        return count != null ? count : 0;
    }

}
