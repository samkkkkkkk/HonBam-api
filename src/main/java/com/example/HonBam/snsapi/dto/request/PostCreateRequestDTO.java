package com.example.HonBam.snsapi.dto.request;

import com.example.HonBam.snsapi.entity.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class PostCreateRequestDTO {
    private String content;
    private List<MediaRequestDTO> mediaList;
}
