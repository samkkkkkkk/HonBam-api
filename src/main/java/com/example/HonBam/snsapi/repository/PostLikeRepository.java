package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.PostLike;
import com.example.HonBam.snsapi.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    boolean existsById(PostLikeId id);

    // 특정 사용자가 누른 좋아요 일괄 조회
    @Query("SELECT pl.id FROM PostLike pl WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    List<PostLikeId> findByUserIdAndPostIdIn(@Param("userId") String userId, @Param("postIds") List<Long> postIds);


    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.id = :id")
    void deleteByIdCustom(PostLikeId id);
    long countByIdPostId(Long postId);
}
