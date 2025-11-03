package com.example.HonBam.snsapi.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CommentUpdateRequestDTO {
    private String content;
}
