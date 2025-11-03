package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.jaas.JaasPasswordCallbackHandler;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByIdAsc(Long postId);

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 특정 댓글의 대댓글 조회
    List<Comment> findByPostIdAndParentIdOrderByCreatedAt(Long postId, Long parentId);

    Long countByPostId(Long postId);

}
