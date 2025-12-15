package com.example.HonBam.upload.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.upload.dto.*;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {

    private final PresignedUrlService presignedUrlService;
    private final UploadService uploadService;

    @GetMapping("/presigned/profile")
    public ResponseEntity<UploadResponseDTO> generateProfilePresignedUrl(
            @RequestBody JoinUploadRequest request
    ) {
        UploadResponseDTO response =
                presignedUrlService.generateProfileUploadUrl(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/presigned")
    public ResponseEntity<List<UploadResponseDTO>> generatePresignedUrls(
            @AuthenticationPrincipal TokenUserInfo userInfo,
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
