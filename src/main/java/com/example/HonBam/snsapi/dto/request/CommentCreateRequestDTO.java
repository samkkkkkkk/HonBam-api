package com.example.HonBam.snsapi.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class CommentCreateRequestDTO {
    private String content;
}
