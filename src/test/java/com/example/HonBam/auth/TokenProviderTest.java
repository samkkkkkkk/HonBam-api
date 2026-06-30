package com.example.HonBam.auth;

import com.example.HonBam.config.AuthProperties;
import com.example.HonBam.exception.JwtAuthException;
import com.example.HonBam.exception.JwtErrorCode;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TokenProvider 단위 테스트 (Spring 컨텍스트 없이 객체 직접 생성).
 * F-1(만료 판별), F-1b(토큰 타입 판별) 회귀 가드를 포함한다.
 */
class TokenProviderTest {

    // HS512 키는 최소 64바이트(512비트) 필요 → 64바이트를 Base64로 인코딩
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

    private TokenProvider tokenProvider;
    private AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.getToken().setAccessExpireMinutes(30);
        authProperties.getToken().setRefreshExpireDays(14);

        tokenProvider = new TokenProvider(authProperties);
        ReflectionTestUtils.setField(tokenProvider, "secretKey", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "base64Secret", true);
        ReflectionTestUtils.setField(tokenProvider, "issuer", "HonBam");
    }

    private User user() {
        return User.builder()
                .id("user-1")
                .role(Role.COMMON)
                .build();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }

    @Test
    @DisplayName("access 토큰 생성 후 검증하면 userId/role이 복원된다")
    void createAndValidateAccessToken() {
        String token = tokenProvider.createAccessToken(user());

        TokenUserInfo info = tokenProvider.validateAccessToken(token);

        assertThat(info.getUserId()).isEqualTo("user-1");
        assertThat(info.getRole()).isEqualTo(Role.COMMON);
    }

    @Test
    @DisplayName("F-1: 만료된 access 토큰 검증 시 ACCESS_TOKEN_EXPIRED 코드의 JwtAuthException")
    void expiredAccessTokenThrowsExpiredCode() {
        // 만료 시간을 과거로 설정해 즉시 만료된 토큰 생성
        authProperties.getToken().setAccessExpireMinutes(-1);
        String expired = tokenProvider.createAccessToken(user());

        assertThatThrownBy(() -> tokenProvider.validateAccessToken(expired))
                .isInstanceOf(JwtAuthException.class)
                .extracting(e -> ((JwtAuthException) e).getErrorCode())
                .isEqualTo(JwtErrorCode.ACCESS_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("F-1b: typ이 access가 아닌 토큰 검증 시 INVALID_TOKEN_TYPE 코드의 JwtAuthException")
    void wrongTokenTypeThrowsInvalidTypeCode() {
        String refreshTyped = Jwts.builder()
                .setSubject("user-1")
                .setIssuer("HonBam")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .addClaims(Map.of("role", Role.COMMON.toString(), "typ", "refresh"))
                .signWith(signingKey(), SignatureAlgorithm.HS512)
                .compact();

        assertThatThrownBy(() -> tokenProvider.validateAccessToken(refreshTyped))
                .isInstanceOf(JwtAuthException.class)
                .extracting(e -> ((JwtAuthException) e).getErrorCode())
                .isEqualTo(JwtErrorCode.INVALID_TOKEN_TYPE);
    }

    @Test
    @DisplayName("변조/서명 불일치 토큰 검증 시 INVALID_JWT 코드의 JwtAuthException")
    void tamperedTokenThrowsInvalidJwt() {
        String token = tokenProvider.createAccessToken(user());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> tokenProvider.validateAccessToken(tampered))
                .isInstanceOf(JwtAuthException.class)
                .extracting(e -> ((JwtAuthException) e).getErrorCode())
                .isEqualTo(JwtErrorCode.INVALID_JWT);
    }

    @Test
    @DisplayName("hashRefreshToken은 동일 입력에 동일 해시를 반환한다")
    void hashRefreshTokenIsDeterministic() {
        String raw = tokenProvider.createRefreshToken(user());

        assertThat(tokenProvider.hashRefreshToken(raw))
                .isEqualTo(tokenProvider.hashRefreshToken(raw))
                .isNotEqualTo(raw);
    }
}
