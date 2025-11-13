package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.Comment;
import com.example.HonBam.userapi.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {
    private Long id;
    private Long postId;
    private String authorId;
    private String authorNickname;
    private String authorProfileUrl;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CommentResponseDTO> children = new ArrayList<>();

    public static CommentResponseDTO from(Comment comment, String authorNickname, String profileImageUrl) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .authorNickname(authorNickname)
                .authorProfileUrl(profileImageUrl)
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

}
