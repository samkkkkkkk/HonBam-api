package com.example.HonBam.upload;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.upload.service.UploadService;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;


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
