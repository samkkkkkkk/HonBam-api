package com.example.HonBam.upload.dto;

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
    private String fileUrl;    // 업로드 후 접근 URL
    private String fileName;
}
