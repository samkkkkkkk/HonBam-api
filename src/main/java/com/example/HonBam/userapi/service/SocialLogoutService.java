package com.example.HonBam.userapi.service;

import com.example.HonBam.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void invalidateUserTokens(String userId) {
        // DB RefreshToken 삭제
        refreshTokenRepository.deleteAllByUserId(userId);

        // Redis RefreshToken 삭제
        redisTemplate.delete("refresh:*" + userId);
    }

    public void loginFromKakao(String accessToken) {

        WebClient webClient = WebClient.create();
        String url = "https://kapi.kakao.com/v1/user/logout";

        try {
            String respone = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("카카오 로그가웃 응답: {}", respone);
        } catch (Exception e) {
            log.info("카카오 로그아웃 실패", e);
        }

    }

    // 네이버 로그아웃(리다이렉트 방식)
    public String getNaverLogout(String redirectUri) {
        log.info("네이버로그아웃");
        return "https://nid.naver.com/nidlogin.logout?returl=" + redirectUri;
    }

}
