package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.UserPay;
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
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
// 역할: 토큰을 발급하고, 서명 위조를 검사하는 객체.
public class TokenProvider {

//    // 서명에 사용할 값 (512비트 이상의 랜덤 문자열)
//    // @Value: properties 형태의 파일의 내용을 읽어서 변수에 대입하는 아노테이션. (yml도 가능)
//    @Value("${jwt.secret}")
//    private String SECRET_KEY;
//
//    // 토큰 생성 메서드
//
//    /**
//     * JSON Web Token을 생성하는 메서드
//     * @param userEntity - 토큰의 내용(클레임)에 포함될 유저 정보
//     * @return - 생성된 JSON을 암호화 한 토큰값
//     */
//    public String createToken(User userEntity) {
//
//        // 토큰 만료시간 생성
//        Date expiry = Date.from(
//                Instant.now().plus(6, ChronoUnit.HOURS)
//        );
//
//        // 토큰 생성
//        /*
//            {
//                "iss": "서비스 이름(발급자)",
//                "exp": "2023-12-27(만료일자)",
//                "iat": "2023-11-27(발급일자)",
//                "email": "로그인한 사람 이메일",
//                "role": "Premium"
//                ...
//                == 서명
//            }
//         */
//
//        return Jwts.builder()
//                .setSubject(userEntity.getId())   // sub 필드에 식별자
//                .claim("role", userEntity.getRole().toString())
//                .setIssuer("HonBam운영자") // iss: 발급자 정보
//                .setIssuedAt(new Date()) // iat: 발급 시간
//                .setExpiration(expiry) // exp: 만료 시간
//                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS512)
//                .compact();
//    }
//
//    /**
//     * 클라이언트가 전송한 토큰을 디코딩하여 토큰의 위조 여부를 확인
//     * 토큰을 json으로 파싱해서 클레임(토큰 정보)을 리턴
//     * @param token
//     * @return - 토큰 안에 있는 인증된 유저 정보를 반환
//     */
//    public TokenUserInfo validateAndGetTokenUserInfo(String token) {
//        Claims claims = Jwts.parserBuilder()
//                // 토큰 발급자의 발급 당시의 서명을 넣어줌
//                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
//                //서명 위조 검사: 위조된 경우에는 예외가 발생합니다.
//                //위조가 되지 않은 경우 payload를 리턴
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        log.info("claims: {}", claims);
//
//        return TokenUserInfo.builder()
//                .userId(claims.getSubject())
//                .role(Role.valueOf(claims.get("role", String.class)))
//                .build();
//    }



    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.base64Secret:true}") // 시크릿이 Base64라면 true, 순수 문자열이면 false
    private boolean base64Secret;

    @Value("${jwt.issuer:HonBam}")
    private String issuer;

    @Value("${jwt.access.minutes:15}")
    private long accessMinutes; // 액세스 토큰 만료(분)

    @Value("${jwt.refresh.days:14}")
    private long refreshDays;   // 리프레시 토큰 만료(일)

    // ====== 공개 API (컨트롤러/필터에서 사용) ======

    /** 기존 컨트롤러 호환: 액세스 토큰 발급 */
    public String createToken(User user) {
        return createAccessToken(user);
    }

    /** 기존 컨트롤러 호환: 리프레시 토큰 발급 */
    public String create(User user) {
        return createRefreshToken(user);
    }

    /** 액세스 토큰 발급 */
    public String createAccessToken(User user) {
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

    /** 리프레시로부터 새 액세스 토큰 발급 (refresh 검증/파싱 후 호출) */
    public String createAccessTokenByInfo(TokenUserInfo info) {
        return buildToken(
                info.getUserId(),
                info.getRole(),
                "access",
                Date.from(Instant.now().plus(accessMinutes, ChronoUnit.MINUTES))
        );
    }

    /** 액세스 토큰 검증 + 파싱 */
    public TokenUserInfo validateAndGetTokenUserInfo(String token) {
        Claims claims = parseAndValidate(token);
        // typ이 access인지 확인(선택: 엄격 모드)
        String typ = claims.get("typ", String.class);
        if (typ != null && !"access".equals(typ)) {
            throw new JwtException("Invalid token type: " + typ);
        }
        return toUserInfo(claims);
    }

    /** 리프레시 토큰 유효성 검사 */
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

    /** 리프레시 토큰 파싱 → 사용자 정보 */
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

        JwtBuilder builder = Jwts.builder()
                .setSubject(userId)               // sub
                .setIssuer(issuer)                // iss
                .setIssuedAt(Date.from(now))      // iat
                .setExpiration(expiry)            // exp
                .addClaims(Map.of(
                        "role", role.toString(),
                        "typ", typ                 // 커스텀 타입: access | refresh
                ))
                .signWith(key, SignatureAlgorithm.HS512);

        return builder.compact();
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














