package com.example.HonBam.aws;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;

    @Service
    @Slf4j
    public class S3Service {

        // s3 버킷을 제어하는 객체
        private S3Client s3;

        @Value("${aws.credentials.accessKey}")
        private String accessKey;

        @Value("${aws.credentials.secretKey}")
        private String secretKey;

        @Value("${aws.region}")
        private String region;

        @Value("${aws.bucketName}")
        private String bucketName;

        // s3에 연결해서 인증을 처리하는 로직
        @PostConstruct // S3Service가 생성될 때 1번만 실행되는 아노테이션
        private void initializeAmazon() {
            // 액세스 키와 시크릿 키를 이용해서 계정 인증 받기
            AwsBasicCredentials credentials
                    = AwsBasicCredentials.create(accessKey, secretKey);

            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }

        /**
         * 버킷에 파일을 업로드하고, 업로드한 버킷의 url 정보를 리턴
         * @param uploadFile - 업로드 할 파일의 실제 raw 데이터
         * @param fileName - 업로드 할 파일명
         * @return - 버킷에 업로드 된 버킷 경로(url)
         */
        public String uploadToS3Bucket(byte[] uploadFile, String fileName) {

            // 업로드 할 파일을 S3 오브젝트로 생성
            PutObjectRequest request
                    = PutObjectRequest.builder()
                    .bucket(bucketName) // 버킷 이름
                    .key(fileName) // 파일명
                    .build();

            // 오브젝트를 버킷에 업로드(위에서 생성한 오브젝트, 업로드 하고자 하는 파일(바이트 배열)
            s3.putObject(request, RequestBody.fromBytes(uploadFile));

            // 업로드 된 파일의 url을 반환
            return s3.utilities()
                    .getUrl(b -> b.bucket(bucketName).key(fileName))
                    .toString();
        }

    }
