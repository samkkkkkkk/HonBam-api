package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByAuthorIdInOrderByIdDesc(Iterable<String> followingIds, Pageable pageable);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    @Query("SELECT P " +
            "fFROM Post p " +
            "WHERE p.authorId IN (" +
            "SELECT f.followingId FROM Follow f WHERE f.followerId = :userId) " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(String userId, Pageable pageable);
}
