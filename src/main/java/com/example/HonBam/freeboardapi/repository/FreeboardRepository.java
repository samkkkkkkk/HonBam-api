package com.example.HonBam.freeboardapi.repository;

import com.example.HonBam.freeboardapi.entity.Freeboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FreeboardRepository extends JpaRepository<Freeboard, Long> {
    @Query("select count(f) > 0 from Freeboard f where f.id = :postId and f.user.id = :userId")
    boolean isPostOwner(@Param("postId") Long postId, @Param("userId") String userId);

    @Modifying
    @Query("delete from Freeboard f where f.id = :postId and f.user.id = :userId")
    int deleteByPostIdAndOwner(@Param("postId") Long postId, @Param("userId") String userId);
}
