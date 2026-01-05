package com.example.HonBam.auth;

import com.example.HonBam.config.AuthProperties;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DigestUtils;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
// 역할: 토큰 발급 및 검증
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.base64Secret:true}")
    private boolean base64Secret;

    @Value("${jwt.issuer:HonBam}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private final AuthProperties authProperties;

    /** 액세스 토큰 발급 */
    public String createAccessToken(User user) {

        Instant now = Instant.now();
        Instant expiry = now.plus(authProperties.getToken().getAccessExpireDuration());

        log.info("createAccessToken - userId: {}", user.getId());
        return buildJwtToken(
                user.getId(),
                user.getRole(),
                "access",
                expiry);
    }

    // Refresh Token 생성
    public String createRefreshToken(User user) {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    // Refresh Token 저장용 해쉬 생성
    public String hashRefreshToken(String refreshToken) {
        return DigestUtils.sha1DigestAsHex(refreshToken);
    }

    // JWT 생성 모듈
    private String buildJwtToken(String userId, Role role, String typ, Instant expiry) {

        return Jwts.builder()
                .setSubject(userId)               // sub = userId
                .setIssuer(issuer)                // iss
                .setIssuedAt(new Date())      // iat
                .setExpiration(Date.from(expiry))            // exp
                .addClaims(Map.of(
                        "role", role.toString(),
                        "typ", typ
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Access Token 검증
    public TokenUserInfo validateAccessToken(String token) {
        Claims claims = parse(token);

        if (!"access".equals(claims.get("typ"))) {
            throw new JwtException("INVALID_TOKEN_TYPE:ACCESS");
        }

        return toUserInfo(claims);
    }

    // JWT 내부 파싱(서명 + 만료 검출)
    private Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtException("TOKEN_EXPIRED");
        } catch (JwtException e) {
            throw new JwtException("INVALID_JWT");
        }
    }

    // Claims -> TokenUserInfo 변환
    private TokenUserInfo toUserInfo(Claims claims) {
        return TokenUserInfo.builder()
                .userId(claims.getSubject())
                .role(Role.valueOf(claims.get("role", String.class)))
                .build();
    }


}
