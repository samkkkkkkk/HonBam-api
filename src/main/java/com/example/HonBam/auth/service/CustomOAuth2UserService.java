package com.example.HonBam.auth.service;

import com.example.HonBam.auth.CustomOAuth2User;
import com.example.HonBam.auth.dto.request.ProviderProfile;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.userapi.entity.LoginProvider;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserProfileMedia;
import com.example.HonBam.userapi.repository.UserProfileMediaRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final UserProfileMediaRepository userProfileMediaRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        ProviderProfile profile = mapProfile(registrationId, attributes);

        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(" 서비스 제공자(네이버/카카오)의 이메일 동의가 필요합니다.");
        }

        LoginProvider provider = LoginProvider.from(registrationId);

        // 이메일 기준 사용자 조회/생성
        User user = userRepository.findByEmail(profile.getEmail()).orElseGet(() -> {
            String dummyPassword = "{noop}" + UUID.randomUUID();
            User created = User.builder()
                    .email(profile.getEmail())
                    .userName(profile.getName())
                    .nickname(profile.getNickname())
                    .password(dummyPassword)
                    .loginProvider(provider)
                    .role(Role.COMMON)
                    .build();
            return userRepository.save(created);
        });

        if (user.getLoginProvider() == null) {
            user.changeLoginProvider(provider);
            userRepository.save(user);
        }

        // 프로필 변경분 반영
        if (profile.getName() != null && !profile.getName().equals(user.getUserName())) {
            user.changeUserName(profile.getName());
            userRepository.save(user);
        }
        // 프로필 이미지 동기화
        if (profile.getImageUrl() != null && !profile.getImageUrl().isBlank()) {
            saveOrUpdateExternalProfileImage(user, profile.getImageUrl());
        }

        // 권한: 사용자 실제 Role
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        String nameAttrKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new CustomOAuth2User(
                user,
                profile.getEmail(),
                Optional.ofNullable(user.getNickname()).orElse(profile.getName()),
                profile.getImageUrl(),
                attributes,
                nameAttrKey,
                authorities
        );
    }

    // [추가 메서드] 외부 이미지 URL을 Media/UserProfileMedia 테이블에 동기화
    private void saveOrUpdateExternalProfileImage(User user, String imageUrl) {
        try {
            Optional<UserProfileMedia> op = userProfileMediaRepository.findByUser(user);

            if (op.isPresent()) {
                // 이미 프로필 이미지가 존재하는 경우 -> URL이 바뀌었는지 확인
                Media existingMedia = op.get().getMedia();
                if (!imageUrl.equals(existingMedia.getFileKey())) {

                    Media newMedia = createExternalMedia(imageUrl);
                    mediaRepository.save(newMedia);
                    
                    // 기존 media 삭제
                    userProfileMediaRepository.delete(op.get());
                    userProfileMediaRepository.flush();

                    UserProfileMedia newLink = UserProfileMedia.builder()
                            .user(user)
                            .media(newMedia)
                            .build();
                    userProfileMediaRepository.save(newLink);
                }
            } else {
                // 프로필 이미지가 없는 경우 -> 새로 생성
                Media newMedia = createExternalMedia(imageUrl);
                mediaRepository.save(newMedia);

                UserProfileMedia newLink = UserProfileMedia.builder()
                        .user(user)
                        .media(newMedia)
                        .build();
                userProfileMediaRepository.save(newLink);
            }
        } catch (Exception e) {
            log.warn("OAuth2 이미지 동기화 실패: {}", e.getMessage());
            // 이미지가 실패해도 로그인은 성공해야 함 -> 예외를 던지지 않음
        }
    }

    private Media createExternalMedia(String imageUrl) {
        return Media.builder()
                .fileKey(imageUrl)
                .contentType("IMAGE")
                .mediaPurpose(MediaPurpose.PROFILE)
                .build();
    }

    private ProviderProfile mapProfile(String registrationId, Map<String, Object> attrs) {
        switch (registrationId) {
            case "naver": {
                Object body = attrs.get("response");
                if (!(body instanceof Map)) {
                    throw new OAuth2AuthenticationException("Naver UserInfo 응답 형식이 올바르지 않습니다.");
                }
                Map<String, Object> r = (Map<String, Object>) body;
                String id = str(r.get("id"));
                String email = str(r.get("email"));
                String name = str(r.get("name"));
                String image = str(r.get("profile_image"));
                return new ProviderProfile(id, email, name, name, image);
            }
            case "kakao": {
                String id = str(attrs.get("id"));
                Map<String, Object> kakao_account = (Map<String, Object>) attrs.get("kakao_account");
                String email = str(kakao_account.get("email"));

                Map<String, Object> profile = (Map<String, Object>) kakao_account.get("profile");
                String nickname = str(profile.get("nickname"));
                String image = str(profile.get("profile_image_url"));

                return new ProviderProfile(id, email, nickname, nickname, image);

            }
            default:
                throw new OAuth2AuthenticationException("미지원 OAuth2 공급자: " + registrationId);
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }


}
