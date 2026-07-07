package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.PostLike;
import com.example.HonBam.snsapi.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    // 특정 사용자가 누른 좋아요 일괄 조회
    @Query("SELECT pl.id FROM PostLike pl WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    List<PostLikeId> findByUserIdAndPostIdIn(@Param("userId") String userId, @Param("postIds") List<Long> postIds);

    // 동시 요청에도 멱등하도록 원자적 INSERT — 이미 존재하면 0 반환 (MySQL 전용)
    @Modifying
    @Query(value = "INSERT IGNORE INTO sns_post_like (user_id, post_id, created_at) " +
                   "VALUES (:userId, :postId, :createdAt)", nativeQuery = true)
    int insertIgnore(@Param("userId") String userId,
                     @Param("postId") Long postId,
                     @Param("createdAt") LocalDateTime createdAt);

    // 삭제된 행 수 반환 — 동시 취소 요청 시 카운트 중복 감소 방지용
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.id.userId = :userId AND pl.id.postId = :postId")
    int deleteLike(@Param("userId") String userId, @Param("postId") Long postId);

}
