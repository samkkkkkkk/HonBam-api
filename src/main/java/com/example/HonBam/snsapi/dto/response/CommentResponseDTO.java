package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter @ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {
    private Long id;
    private Long postId;
    private String authorId;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponseDTO from(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

}
