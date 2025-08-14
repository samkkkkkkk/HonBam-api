package com.example.HonBam.freeboardapi.repository;

import com.example.HonBam.freeboardapi.dto.response.FreeboardCommentResponseDTO;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FreeboardCommentRepository extends JpaRepository<FreeboardComment, Long> {
    @Query("select c.id, c.comment, c.createTime, c.writer, u.nickname from FreeboardComment c join c.user u where c.freeboard.id = :postId")
    List<FreeboardCommentResponseDTO> findCommentsWithWriterByPostId(Long postId);

    @Query("select count(c) > 0 from FreeboardComment c where c.id = :commentId and c.user.id = :userId")
    boolean isCommentOwner(@Param("commentId") Long commentId, @Param("userId") String userId);

    @Modifying
    @Query("delete from FreeboardComment c where c.id = :commentId and c.user.id = :userId")
    int deleteByCommentIdAndOwner(@Param("commentId") Long commentId, @Param("userId") String userId);

}
