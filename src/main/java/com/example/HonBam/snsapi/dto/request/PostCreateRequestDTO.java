package com.example.HonBam.snsapi.dto.request;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class PostCreateRequestDTO {
    private String content;
    private List<String> imageUrls;
}
