package com.example.HonBam.snsapi.service;

import com.example.HonBam.notification.event.LikeCreateEvent;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLike;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 좋아요 추가
    public void addLike(String userId, Long postId) {
        PostLikeId id = new PostLikeId(userId, postId);
        if (!postLikeRepository.existsById(id)) {
            postLikeRepository.save(new PostLike(id, LocalDateTime.now()));
        }

        // 게시글 작성자 조회 (Post.authorId)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        String receivedId = post.getAuthorId();

        // 비동기 알림 이벤트 발생
        eventPublisher.publishEvent(new LikeCreateEvent(userId, postId, receivedId));

    }

    // 좋아요 취소
    public void removeLike(String userId, Long postId) {
        PostLikeId id = new PostLikeId(userId, postId);
        postLikeRepository.deleteById(id);
    }

    // 좋아요 여부 확인
    public boolean isLiked(String userId, Long postId) {
        return postLikeRepository.existsById(new PostLikeId(userId, postId));
    }

    // 좋아요 수 조회
    public Long getLikeCount(Long postId) {
        return postLikeRepository.countByIdPostId(postId);
    }

}
