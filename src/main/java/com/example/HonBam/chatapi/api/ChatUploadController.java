package com.example.HonBam.chatapi.api;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatUploadController {

    private final UploadService uploadService;

    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadChatFile(@RequestParam("file") MultipartFile file) {
        UploadResponseDTO result = uploadService.uploadFile(file);
        return ResponseEntity.ok(result);
    }
}
