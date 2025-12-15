package com.example.HonBam.upload.dto;

import com.example.HonBam.upload.entity.MediaPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadCompleteRequest {
    // S3 object key
    private String fileKey;
    private MediaPurpose purpose;
}
