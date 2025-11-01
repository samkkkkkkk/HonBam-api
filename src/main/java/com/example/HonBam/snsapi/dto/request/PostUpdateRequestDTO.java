package com.example.HonBam.snsapi.dto.request;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PostUpdateRequestDTO {
    private String content;
    List<String> imageUrls;
}
