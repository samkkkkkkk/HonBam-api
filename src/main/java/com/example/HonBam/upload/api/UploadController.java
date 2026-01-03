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
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {

    private final PresignedUrlService presignedUrlService;
    private final UploadService uploadService;

    @PostMapping("/presigned/profile")
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
    public ResponseEntity<List<MediaResponseDTO>> completeUpload(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody List<UploadCompleteRequest> requests
    ) {
        List<Media> medias = uploadService.completeUpload(userInfo.getUserId(), requests);
        return ResponseEntity.ok(medias.stream().map(MediaResponseDTO::from).collect(Collectors.toList()));
    }

}
