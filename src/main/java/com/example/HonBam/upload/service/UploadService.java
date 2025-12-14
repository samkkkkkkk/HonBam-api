package com.example.HonBam.upload.service;

import com.example.HonBam.upload.dto.UploadCompleteRequest;
import com.example.HonBam.upload.entity.Media;
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

        if (mediaRepository.existsByFileKey(req.getFileKey())) {
            throw new IllegalStateException("이미 등록된 파일입니다.");
        }

        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(req.getFileKey())
                        .build()
        );

        validatePolicy(req, head);

        Media media = Media.builder()
                .uploaderId(uploaderId)
                .mediaPurpose(req.getPurpose())
                .fileKey(req.getFileKey())
                .contentType(head.contentType())
                .fileSize(head.contentLength())
                .build();

        return mediaRepository.save(media);
    }

    private void validatePolicy(
            UploadCompleteRequest req,
            HeadObjectResponse head
    ) {
        String contentType = head.contentType();

        switch (req.getPurpose()) {
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
