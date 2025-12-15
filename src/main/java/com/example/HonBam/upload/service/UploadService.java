package com.example.HonBam.upload.service;

import com.example.HonBam.upload.dto.UploadCompleteRequest;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Service
@RequiredArgsConstructor
public class UploadService {


    private final MediaRepository mediaRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Transactional
    public Media completeUpload(String uploaderId, UploadCompleteRequest req) {
        return createMedia(uploaderId, req.getFileKey(), req.getPurpose());
    }

    @Transactional
    public Media createMedia(String uploaderId, String fileKey, MediaPurpose purpose) {

        // 1. 중복 체크
        if (mediaRepository.existsByFileKey(fileKey)) {
            // 이미 저장된 경우 해당 미디어를 반환 (채팅 재전송 등을 고려)
            return mediaRepository.findByFileKey(fileKey)
                    .orElseThrow(() -> new IllegalStateException("파일 데이터 불일치"));
        }

        // 2. S3 실제 존재 여부 및 메타데이터 확인 (기존 로직 재사용!)
        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileKey)
                        .build()
        );

        // 3. 정책 검증 (기존 로직 재사용!)
        validatePolicy(purpose, head.contentType());

        // 4. Media 엔티티 생성 및 저장
        Media media = Media.builder()
                .uploaderId(uploaderId)
                .mediaPurpose(purpose)
                .fileKey(fileKey)
                .contentType(head.contentType())
                .fileSize(head.contentLength())
                .build();

        return mediaRepository.save(media);
    }

    private void validatePolicy(MediaPurpose purpose, String contentType) {
        switch (purpose) {
            case PROFILE:
                if (!contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("프로필은 이미지 파일만 허용");
                }
                break;

            case POST:
                // image/*, video/* 모두 허용
                if (!(contentType.startsWith("image/")
                        || contentType.startsWith("video/"))) {
                    throw new IllegalArgumentException("게시글은 이미지 또는 영상만 허용");
                }
                break;

            case CHAT:
                break;
        }
    }
}
