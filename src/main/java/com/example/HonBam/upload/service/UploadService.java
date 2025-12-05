package com.example.HonBam.upload.service;

import com.example.HonBam.upload.StorageService;
import com.example.HonBam.upload.dto.UploadResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final StorageService storageService;

    public UploadResponseDTO uploadFile(MultipartFile file) {
        String url = storageService.upload(file);

        return UploadResponseDTO.builder()
                .url(url)
                .fileName(url.substring(url.lastIndexOf("/") + 1))
                .build();
    }
}
