package com.example.HonBam.upload.dto;

import com.example.HonBam.upload.entity.Media;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MediaResponseDTO {

    private Long mediaId;
    private String fileKey;
    private String contentType;
    private long fileSize;

    public static MediaResponseDTO from(Media media) {
        return new MediaResponseDTO(
                media.getId(),
                media.getFileKey(),
                media.getContentType(),
                media.getFileSize()
        );
    }
}