package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByIdFollowerId(String followerId);
}
