package com.example.HonBam.snsapi.dto.response;

import lombok.*;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostListResponseDTO {

    private List<PostResponseDTO> postList;
    private int count;

}
