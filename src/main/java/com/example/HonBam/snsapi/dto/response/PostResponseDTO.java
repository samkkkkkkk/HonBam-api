package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.SnsMedia;
import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class PostResponseDTO {
    private Long id;
    private String authorId;
    private String authorNickname;
    private String authorProfileUrl;
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean likeByMe;

    public static PostResponseDTO from(Post post, boolean likeByMe, String nickname, String profileUrl) {
        return PostResponseDTO.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorNickname(nickname)
                .authorProfileUrl(profileUrl)
                .content(post.getContent())
                .imageUrls(extractImageUrls(post))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeByMe(likeByMe)
                .build();
    }

    private static List<String> extractImageUrls(Post post) {
        return post.getMediaList().stream()
                .map(SnsMedia::getFileUrl)
                .collect(Collectors.toList());
    }

}
