package com.example.HonBam.auth;

import com.example.HonBam.auth.dto.ProviderProfile;
import com.example.HonBam.userapi.entity.LoginProvider;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        ProviderProfile profile = mapProfile(registrationId, attributes);

        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(" 네이버 개발자 콘솔의 scope와 서비스 정책을 확인하세요.");
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
                    .profileImg(profile.getImageUrl())
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
        boolean changed = false;
        if (profile.getName() != null && !profile.getName().equals(user.getUserName())) {
            user.changeUserName(profile.getName());
            changed = true;
        }
        if (profile.getImageUrl() != null && !profile.getImageUrl().equals(user.getProfileImg())) {
            user.changeProfileImage(profile.getImageUrl());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
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
                user.getProfileImg(),
                attributes,
                nameAttrKey,
                authorities
        );
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
