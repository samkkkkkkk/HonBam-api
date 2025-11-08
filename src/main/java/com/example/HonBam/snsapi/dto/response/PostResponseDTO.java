package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
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
    private String nickname;
    private String profileImage;
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean likeByMe;

    public static PostResponseDTO from(Post post, boolean likeByMe, User user) {
        return PostResponseDTO.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImg())
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrlsJson()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeByMe(likeByMe)
                .build();
    }

    /**
     * 이미지 URL JSON 문자열을 List<String>으로 변환
     * 예: ["a.jpg","b.jpg"] → List.of("a.jpg", "b.jpg")
     */
    private static List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


}
