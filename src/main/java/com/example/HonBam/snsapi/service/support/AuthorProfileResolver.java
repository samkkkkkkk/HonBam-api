package com.example.HonBam.snsapi.service.support;

import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserProfileMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// 작성자 프로필 이미지의 presigned URL 조회 (snsapi 서비스 공용)
@Component
@RequiredArgsConstructor
public class AuthorProfileResolver {

    private final UserProfileMediaRepository userProfileMediaRepository;
    private final PresignedUrlService presignedUrlService;

    public String resolve(User author) {
        return userProfileMediaRepository.findByUser(author)
                .map(pm -> presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey()))
                .orElse(null);
    }

    // authorId -> presigned URL 일괄 조회 (프로필이 없는 작성자는 미포함)
    public Map<String, String> resolveUrlMap(Collection<String> authorIds) {
        return userProfileMediaRepository.findByUser_IdIn(authorIds).stream()
                .collect(Collectors.toMap(
                        pm -> pm.getUser().getId(),
                        pm -> presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey()),
                        (existing, replacement) -> existing
                ));
    }
}
