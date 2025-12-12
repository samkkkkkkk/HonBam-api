package com.example.HonBam.snsapi.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class MediaRequestDTO {
    private String fileKey;
    private String fileUrl;
    private String contentType;
    private long fileSize;
    private int sortOrder;
}
