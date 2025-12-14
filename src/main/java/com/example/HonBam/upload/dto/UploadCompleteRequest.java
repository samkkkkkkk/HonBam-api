package com.example.HonBam.upload.dto;

import com.example.HonBam.upload.entity.MediaPurpose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompleteRequest {
    // S3 object key
    private String fileKey;

    // 클라이언트가 알고 있는 원본 파일명
    private String originalFileName;

    private String contentType;

    private long fileSize;

    private MediaPurpose purpose;
}
