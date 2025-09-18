package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
// 역할: 토큰 발급 및 검증
public class TokenProvider {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.base64Secret:true}")
    private boolean base64Secret;

    @Value("${jwt.issuer:HonBam}")
    private String issuer;

    @Value("${jwt.access.minutes:15}")
    private long accessMinutes; // 액세스 토큰 만료(분)

    @Value("${jwt.refresh.days:14}")
    private long refreshDays;   // 리프레시 토큰 만료(일)


    /** 액세스 토큰 발급 */
    public String createAccessToken(User user) {
        log.info("createAccessToken - userId: {}", user.getId());
        return buildToken(
                user.getId(),
                user.getRole(),
                "access",
                Date.from(Instant.now().plus(accessMinutes, ChronoUnit.MINUTES))
        );
    }

    /** 리프레시 토큰 발급 */
    public String createRefreshToken(User user) {
        return buildToken(
                user.getId(),
                user.getRole(),
                "refresh",
                Date.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS))
        );
    }

    /** 리프레시 토큰 기반 액세스 토큰 재발급 */
    public String createAccessTokenByInfo(TokenUserInfo info) {
        return buildToken(
                info.getUserId(),
                info.getRole(),
                "access",
                Date.from(Instant.now().plus(accessMinutes, ChronoUnit.MINUTES))
        );
    }

    /** 액세스 토큰 검증 + 사용자 정보 추출 */
    public TokenUserInfo validateAndGetTokenUserInfo(String token) {
        Claims claims = parseAndValidate(token);
        String typ = claims.get("typ", String.class);
        if (typ != null && !"access".equals(typ)) {
            throw new JwtException("Invalid token type: " + typ);
        }
        return toUserInfo(claims);
    }

    /** 리프레시 토큰 유효성 검증 */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseAndValidate(token);
            String typ = claims.get("typ", String.class);
            return "refresh".equals(typ);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /** 리프레시 토큰에서 사용자 정보 추출 */
    public TokenUserInfo parseRefreshToken(String token) {
        Claims claims = parseAndValidate(token);
        String typ = claims.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new JwtException("Not a refresh token");
        }
        return toUserInfo(claims);
    }

    // ====== 내부 유틸 ======

    private String buildToken(String userId, Role role, String typ, Date expiry) {
        Instant now = Instant.now();
        SecretKey key = getSigningKey();

        return Jwts.builder()
                .setSubject(userId)               // sub = userId
                .setIssuer(issuer)                // iss
                .setIssuedAt(Date.from(now))      // iat
                .setExpiration(expiry)            // exp
                .addClaims(Map.of(
                        "role", role.toString(),
                        "typ", typ
                ))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims parseAndValidate(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        if (base64Secret) {
            byte[] bytes = Base64.getDecoder().decode(SECRET_KEY);
            return Keys.hmacShaKeyFor(bytes);
        }
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    private TokenUserInfo toUserInfo(Claims claims) {
        return TokenUserInfo.builder()
                .userId(claims.getSubject())
                .role(Role.valueOf(claims.get("role", String.class)))
                .build();
    }
}
