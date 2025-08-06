package com.example.HonBam.freeboardapi.repository;

import com.example.HonBam.freeboardapi.dto.response.FreeboardCommentResponseDTO;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FreeboardCommentRepository extends JpaRepository<FreeboardComment, Long> {
    @Query("select c, u.nickname from FreeboardComment c join c.user u where c.postId = :postId")
    List<FreeboardCommentResponseDTO> findCommentsWithNicknameByPostId(Long postId);
}
