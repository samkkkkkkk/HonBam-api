package com.example.HonBam.upload.dto;

import com.example.HonBam.upload.entity.MediaPurpose;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class FileUploadRequest {
    private String fileName;
    private String contentType;
    private MediaPurpose mediaPurpose;
}
