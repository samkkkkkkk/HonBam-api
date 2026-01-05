package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Follow;
import com.example.HonBam.snsapi.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsById(FollowId id);

    void deleteById(FollowId id);
    long countByIdFollowerId(String followerId);

    long countByIdFollowingId(String followingId);

    List<Follow> findByIdFollowerId(String followerId);

    List<Follow> findAllByIdFollowingId(String userId);

    List<Follow> findAllByIdFollowerId(String userId);

}
