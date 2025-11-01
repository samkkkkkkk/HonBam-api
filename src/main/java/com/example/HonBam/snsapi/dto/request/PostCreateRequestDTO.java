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
    private List<String> imageUrls;

    public Post toEntity(String authorId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> safeList = (imageUrls == null) ? Collections.emptyList() : imageUrls;

            String json = mapper.writeValueAsString(safeList);
            return Post.builder()
                    .authorId(authorId)
                    .content(content)
                    .imageUrlsJson(json)
                    .likeCount(0)
                    .commentCount(0)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("게시물 생성 중 오류 발생", e);
        }
    }


}
