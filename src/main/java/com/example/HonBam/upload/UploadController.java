package com.example.HonBam.upload;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.upload.dto.FileUploadRequest;
import com.example.HonBam.upload.dto.MediaResponseDTO;
import com.example.HonBam.upload.dto.UploadCompleteRequest;
import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {

    private final PresignedUrlService presignedUrlService;
    private final UploadService uploadService;

    @PostMapping("/presigned")
    public ResponseEntity<List<UploadResponseDTO>> generatePresignedUrls(
            @RequestBody List<FileUploadRequest> requests
    ) {
        return ResponseEntity.ok(
                presignedUrlService.generateUploadUrls(requests)
        );
    }

    @PostMapping("/complete")
    public ResponseEntity<MediaResponseDTO> completeUpload(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody UploadCompleteRequest request
    ) {
        Media media = uploadService.completeUpload(userInfo.getUserId(), request);
        return ResponseEntity.ok(MediaResponseDTO.from(media));
    }
}
