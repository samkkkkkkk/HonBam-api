package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Follow;
import com.example.HonBam.snsapi.entity.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    long countByIdFollowerId(String followerId);

    long countByIdFollowingId(String followingId);

    Page<Follow> findAllByIdFollowingId(String userId, Pageable pageable);

    Page<Follow> findAllByIdFollowerId(String userId, Pageable pageable);

    // 동시 요청에도 멱등하도록 원자적 INSERT — 이미 존재하면 0 반환 (MySQL 전용)
    @Modifying
    @Query(value = "INSERT IGNORE INTO sns_follow (follower_id, following_id, created_at) " +
                   "VALUES (:followerId, :followingId, :createdAt)", nativeQuery = true)
    int insertIgnore(@Param("followerId") String followerId,
                     @Param("followingId") String followingId,
                     @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.id.followerId = :followerId AND f.id.followingId = :followingId")
    int deleteFollow(@Param("followerId") String followerId, @Param("followingId") String followingId);

}
