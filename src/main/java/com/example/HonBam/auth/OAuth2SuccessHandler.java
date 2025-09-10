package com.example.HonBam.auth;

import com.example.HonBam.auth.entity.RefreshToken;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2AuthorizedClientService clientService;

    @Value("${app.oauth2.redirect.success}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        // 1. JWT 발급( TokenProvider 시그니처 사용)
        String access = tokenProvider.createToken(user);
        String refresh = tokenProvider.createRefreshToken(user);

        // 2. RefreshToken DB 저장
        String refreshHash = DigestUtils.sha256Hex(refresh);
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .revoked(false)
                .expiredAt(LocalDateTime.now().plusDays(14))
                .deviceInfo("User-Agent")
                .build();
        refreshTokenRepository.save(entity);

        // 3. RefreshToken Redis 저장
        redisTemplate.opsForValue().set("refresh:" + refresh, user.getId(), 14, TimeUnit.DAYS);

        // 4.SNS AccessToken Redis 저장
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client =
                    clientService.loadAuthorizedClient(
                            oauth2Token.getAuthorizedClientRegistrationId(),
                            oauth2Token.getName()
                    );

            if (client != null && client.getAccessToken() != null) {
                String accessToken = client.getAccessToken().getTokenValue();
                String refreshToken = client.getRefreshToken() != null
                        ? client.getRefreshToken().getTokenValue()
                        : "";

                log.info("SNS AccessToken = {}", accessToken);
                log.info("SNS RefreshToken = {}", refreshToken);

                String key = "social:token:" + user.getId();
                Map<String, String> tokenInfo = Map.of(
                        "provider", oauth2Token.getAuthorizedClientRegistrationId(),
                        "accessToken", accessToken,
                        "refreshToken", refreshToken
                );

                long ttl = ChronoUnit.SECONDS.between(
                        Instant.now(),
                        client.getAccessToken().getExpiresAt()
                );

                redisTemplate.opsForHash().putAll(key, tokenInfo);
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        }
        
        // 5. JWT 쿠키 저장
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createHttpOnly("access_token", access, Duration.ofMinutes(15)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createHttpOnly("refresh_token", refresh, Duration.ofDays(14)).toString());

        // 3. 프론트로 리다이렉트
        response.sendRedirect(successRedirectUrl);
    }
}
