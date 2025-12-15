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
public class UploadResponseDTO {
    private String uploadUrl;  // presigned URL (PUT 업로드용)
    private String fileKey;
    private String fileName;
    private MediaPurpose purpose;
}
