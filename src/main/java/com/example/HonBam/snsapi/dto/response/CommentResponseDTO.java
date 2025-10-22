package com.example.HonBam.snsapi.dto.response;

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
    private LocalDateTime createdAt;
}
