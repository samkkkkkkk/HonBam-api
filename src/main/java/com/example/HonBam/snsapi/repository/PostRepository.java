package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByAuthorIdInOrderByIdDesc(Iterable<String> followingIds, Pageable pageable);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findAllByOrderByLikeCountDesc(Pageable pageable);

    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.authorId IN (" +
            "SELECT f.id.followingId  FROM Follow f WHERE f.id.followingId  = :userId) " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFeedPosts(String userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = :newCount WHERE p.id = :postId")
    void updateLikeCount(Long postId, int newCount);

    // commentCount
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    int increaseCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    int decreaseCommentCount(@Param("postId") Long postId);


    // 좋아요 수정
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    int increaseLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    int decreaseLikeCount(@Param("postId") Long postId);

    @Query("SELECT p.likeCount FROM Post p WHERE p.id = :postId")
    Integer findLikeCount(@Param("postId") Long postId);

    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.createdAt BETWEEN :start AND :end " +
            "AND p.imageUrlsJson IS NOT NULL " +
            "AND p.imageUrlsJson <> '' " +
            "AND p.imageUrlsJson <> '[]' " +
            "ORDER BY p.likeCount DESC, p.createdAt DESC")
    List<Post> findTodayShotsOrderByLikes(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

}
