package com.example.HonBam.upload.service;

import com.example.HonBam.upload.dto.FileUploadRequest;
import com.example.HonBam.upload.dto.JoinUploadRequest;
import com.example.HonBam.upload.dto.UploadResponseDTO;
import com.example.HonBam.upload.entity.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public List<UploadResponseDTO> generateUploadUrls(List<FileUploadRequest> requests) {
        return requests.stream()
                .map(req -> generateUploadUrl(req.getFileName(), req.getContentType(), req.getMediaPurpose()))
                .collect(Collectors.toList());
    }

    public UploadResponseDTO generateUploadUrl(String fileName, String contentType, MediaPurpose mediaPurpose) {
        // 파일 경로 생성 (예: 2025-12-07/uuid-test.png)
        String objectName = buildFileKey(mediaPurpose.getPrefix(), fileName);

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
                .fileKey(objectName)
                .fileName(fileName)
                .purpose(mediaPurpose)
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


    public UploadResponseDTO generateProfileUploadUrl(JoinUploadRequest request) {
        if (!request.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("프로필은 이미지 파일만 허용됩니다.");
        }

        String objectKey = buildFileKey("profile", request.getFileName());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(3)) // 짧게
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presigned =
                presigner.presignPutObject(presignRequest);

        return UploadResponseDTO.builder()
                .uploadUrl(presigned.url().toString())
                .fileKey(objectKey)
                .build();

    }

    private String buildFileKey(String prefix, String originalFileName) {
        String ext = extractExtension(originalFileName);
        return String.format(
                "%s/%s/%s%s",
                prefix,
                LocalDate.now(),
                UUID.randomUUID(),
                ext
        );
    }

    private String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return (idx > 0) ? fileName.substring(idx) : "";
    }

}
