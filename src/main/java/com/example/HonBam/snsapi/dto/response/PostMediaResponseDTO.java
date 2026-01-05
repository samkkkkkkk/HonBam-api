package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.snsapi.entity.PostMedia;
import com.example.HonBam.upload.entity.Media;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostMediaResponseDTO {

    private Long mediaId;
    private String url;
    private String contentType;
    private int sortOrder;

    public static PostMediaResponseDTO from(PostMedia postMedia, String mediaUrl) {
        Media media = postMedia.getMedia();
        return new PostMediaResponseDTO(
                media.getId(),
                mediaUrl,
                media.getContentType(),
                postMedia.getSortOrder()
        );
    }
}