package com.example.HonBam.upload;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {


    private final PresignedUrlService presignedUrlService;

    @GetMapping("/presigned")
    public ResponseEntity<?> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        UploadResponseDTO dto = presignedUrlService.generateUploadUrl(fileName, contentType);
        return ResponseEntity.ok(dto);
    }
}
