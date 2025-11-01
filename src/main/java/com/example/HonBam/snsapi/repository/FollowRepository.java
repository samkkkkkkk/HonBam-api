package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Follow;
import com.example.HonBam.snsapi.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsById(FollowId id);

    long countById_FollowerId(String followerId);

    List<Follow> findByIdFollowerId(String followerId);
}
