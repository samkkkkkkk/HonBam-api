package com.example.HonBam.userapi.service;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.exception.DuplicateEmailException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.upload.dto.UploadCompleteRequest;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.upload.service.UploadService;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.auth.dto.response.LoginResponseDTO;
import com.example.HonBam.userapi.dto.response.UserInfoResponseDTO;
import com.example.HonBam.userapi.dto.response.UserSignUpResponseDTO;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserProfileMedia;
import com.example.HonBam.userapi.repository.UserProfileMediaRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UploadService uploadService;
    private final UserProfileMediaRepository userProfileMediaRepository;
    private final PresignedUrlService presignedUrlService;

    // 회원 가입 처리
    public UserSignUpResponseDTO create(
            final UserRequestSignUpDTO dto
    ) {
        String email = dto.getEmail();

        if (isDuplicate(email, "email")) {
            log.warn("이메일이 중복되었습니다. - {}", email);
            throw new DuplicateEmailException("중복된 이메일 입니다.");
        }

        String encoded = passwordEncoder.encode(dto.getPassword());
        dto.setPassword(encoded);

        User saved = userRepository.save(dto.toEntity());

        // 프로필 이미지 처리
        if (dto.getProfileImageKey() != null && !dto.getProfileImageKey().isBlank()) {
            linkProfileImage(saved, dto.getProfileImageKey());
        }
        log.info("회원 가입 정상 수행됨! - saved user - {}", saved);

        return new UserSignUpResponseDTO(saved);
    }

    private void linkProfileImage(User user, String fileKey) {
        try {
            // 서버에서 S3로 요청 보내기
            UploadCompleteRequest uploadRequest = UploadCompleteRequest.builder()
                    .fileKey(fileKey)
                    .purpose(MediaPurpose.PROFILE) // 목적은 PROFILE로 고정
                    .build();

            // media 생성, DB에 저장
            Media savedMedia = uploadService.completeOne(user.getId(), uploadRequest);

            // UserProfileMedia 연결 및 저장
            UserProfileMedia userProfileMedia = UserProfileMedia.builder()
                    .user(user)
                    .media(savedMedia)
                    .build();

            userProfileMediaRepository.save(userProfileMedia);

        } catch (Exception e) {
            log.warn("프로필 이미지 연결 실패: {}", e.getMessage());
             throw new RuntimeException("프로필 설정 중 오류 발생");
        }
    }

    public boolean isDuplicate(String target, String value) {
        if (target.equals("userId")) {
            return userRepository.existsByNickname(value);
        } else if (target.equals("email")) {
            return userRepository.existsByEmail(value);
        }
        return false;
    }


    public LoginResponseDTO promoteToPayPremium(String userId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        foundUser.changeRole(Role.PREMIUM);
        User saved = userRepository.save(foundUser);
        String token = tokenProvider.createAccessToken(saved);
        return new LoginResponseDTO(saved);
    }


    public void delete(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
    }

    public UserInfoResponseDTO getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return new UserInfoResponseDTO(user);
    }


    public String getProfileUrl(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        return userProfileMediaRepository.findByUser(user)
                .map(profileMedia -> {
                    String fileKey = profileMedia.getMedia().getFileKey();
                    // 1. 카카오 등 외부 URL인 경우
                    if (fileKey.startsWith("http")) {
                        return fileKey;
                    }
                    // 2. S3 파일인 경우 (Presigned URL 생성)
                    return presignedUrlService.generatePresignedGetUrl(fileKey);
                })
                .orElse(null); // 프로필 사진이 없으면 null 반환
    }
}


