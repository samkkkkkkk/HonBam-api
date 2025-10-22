package com.example.HonBam.snsapi.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class PostResponseDTO {
    private Long id;
    private String authorId;
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean likeByMe;
}
