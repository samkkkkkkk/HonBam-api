package com.example.HonBam.freeboardapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FreeboardCommentResponseDTO {
    private Long commentId;
    private String comment;
    private String nickname;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
