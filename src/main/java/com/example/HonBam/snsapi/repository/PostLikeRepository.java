package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    long countByIdPostId(Long postId);
}
