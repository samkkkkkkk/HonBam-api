package com.example.HonBam.upload;

import com.example.HonBam.upload.dto.FileUploadRequest;
import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;


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

    @PostMapping("/presigned-urls")
    public ResponseEntity<List<UploadResponseDTO>> getPresignedUrls(
            @RequestBody List<FileUploadRequest> requests
    ) {
        List<UploadResponseDTO> dto = presignedUrlService.generateUploadUrls(requests);
        return ResponseEntity.ok(dto);
    }
}
