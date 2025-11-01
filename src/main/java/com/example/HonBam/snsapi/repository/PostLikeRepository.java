package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsById(PostLike id);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.id = :id")
    void deleteByIdCustom(PostLike id);

    long countById_PostId(Long postId);
}
