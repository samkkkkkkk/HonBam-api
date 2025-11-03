package com.example.HonBam.snsapi.service;

import com.example.HonBam.snsapi.entity.PostLike;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final PostLikeRepository postLikeRepository;


    // 좋아요 추가
    public void addLike(String userId, Long postId) {
        PostLikeId id = new PostLikeId(userId, postId);
        if (!postLikeRepository.existsById(id)) {
            postLikeRepository.save(new PostLike(id, LocalDateTime.now()));
        }
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
