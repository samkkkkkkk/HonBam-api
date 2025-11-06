package com.example.HonBam.upload;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/api")
@Slf4j
public class UploadController {

    @Value("${upload.path}")
    private String uploadRootPath;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "빈 배열입니다."));
        }

        try {
            String today = LocalDate.now().toString();
            Path uploadPath = Paths.get(uploadRootPath, today);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";

            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex);
            }

            String newFilename = UUID.randomUUID() + extension;
            File savedPath = uploadPath.resolve(newFilename).toFile();

            file.transferTo(savedPath);

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .build()
                    .toUriString();

            // 접근 가능한 URL 생성
            String fileUrl = String.format("%s/uploads/%s/%s", baseUrl, today, newFilename);

            log.info("파일 업로드 성공: {}", fileUrl);
            UploadResponseDTO response = UploadResponseDTO.builder()
                    .url(fileUrl)
                    .fileName(newFilename)
                    .build();


            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }

    }
}
