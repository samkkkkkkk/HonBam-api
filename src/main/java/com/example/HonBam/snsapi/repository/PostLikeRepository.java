package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.PostLike;
import com.example.HonBam.snsapi.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existByIds(PostLikeId id);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.id = :id")
    void deleteByIdCustom(PostLikeId id);

    long countByIdPostId(Long postId);
}
