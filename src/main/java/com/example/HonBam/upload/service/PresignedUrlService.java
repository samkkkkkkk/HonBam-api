package com.example.HonBam.upload.service;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public UploadResponseDTO generateUploadUrl(String fileName, String contentType) {
        // 파일 경로 생성 (예: 2025-12-07/uuid-test.png)
        String objectName = LocalDate.now() + "/" + UUID.randomUUID() + "-" + fileName;

        // S3에 저장될 파일 정보
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .contentType(contentType)
                .build();

        // Presigned URL 생성 요청
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        // 최종 presigned URL 생성
        PresignedPutObjectRequest presignedRequest =
                presigner.presignPutObject(presignRequest);

        // 클라이언트가 직접 PUT할 주소
        String uploadUrl = presignedRequest.url().toString();

        // 클라이언트가 GET할 url 생성
        String downloadUrl = generatePresignedGetUrl(objectName);

        return UploadResponseDTO.builder()
                .uploadUrl(uploadUrl)
                .fileUrl(downloadUrl)
                .fileName(objectName)
                .build();
    }

    public String generatePresignedGetUrl(String objectName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

        return presigned.url().toString();
    }
}
