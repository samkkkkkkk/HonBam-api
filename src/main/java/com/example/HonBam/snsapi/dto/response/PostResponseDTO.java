package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class PostResponseDTO {


    private Long postId;
    private String content;
    private int likeCount;
    private int commentCount;
    private boolean liked;

    private String authorNickname;
    private String authorProfileUrl;

    private List<PostMediaResponseDTO> medias;
    private LocalDateTime createdAt;

    public static PostResponseDTO from(
            Post post,
            boolean liked,
            String authorNickname,
            String authorProfileUrl,
            List<PostMediaResponseDTO> medialist
    ) {

        return PostResponseDTO.builder()
                .postId(post.getId())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .authorNickname(authorNickname)
                .authorProfileUrl(authorProfileUrl)
                .medias(medialist)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
