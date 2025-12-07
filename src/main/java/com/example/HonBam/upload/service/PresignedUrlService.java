package com.example.HonBam.upload.service;

import com.example.HonBam.upload.dto.UploadResponseDTO;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner presigner;

    @Value("${minio.bucket}")
    private String bucket;


    @Value("${aws.s3.region}")
    private String region;

    @Value("${minio.endpoint}")
    private String endpoint;

    public UploadResponseDTO generateUploadUrl(String fileName, String contentType) {
        String objectName = LocalDate.now() + "/" + UUID.randomUUID() + "-" + fileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest =
                presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedRequest.url().toString();
        // AWS S3 실제 파일 접근 URL
        String fileUrl = String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                region,
                objectName
        );
        return UploadResponseDTO.builder()
                .uploadUrl(uploadUrl)
                .fileUrl(fileUrl)
                .fileName(objectName)
                .build();
    }
}
