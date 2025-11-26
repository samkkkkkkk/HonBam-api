package com.example.HonBam.userapi.service;

import com.example.HonBam.auth.entity.RefreshToken;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 사용자(userId)의 모든 Refresh Token 폐기
     * 1) DB RefreshToken 조회 (tokenHash 확보)
     * 2) Redis refresh:{hash} 삭제
     * 3) DB RefreshToken 삭제
     */
    @Transactional
    public void invalidateUserTokens(String userId) {
        // DB에서 RefreshToken 목록 조회
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);

        // redis에 저장된 refresh:{hash} 삭제
        for (RefreshToken token : tokens) {
            String redisKey = "refresh:" + token.getTokenHash();
            redisTemplate.delete(redisKey);
            log.info("Invalidated redis refresh token: {}", redisKey);

            token.revoke();
        }

        // DB RefreshToken 삭제
        refreshTokenRepository.saveAll(tokens);
        log.info("DB refresh tokens removed for userId: {}", userId);
    }

    public void loginFromKakao(String accessToken) {

        WebClient webClient = WebClient.create();
        String url = "https://kapi.kakao.com/v1/user/logout";

        try {
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("카카오 로그아웃 응답: {}", response);
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
