package com.example.HonBam.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {
    // application.yml에서 주입해서 운영/개발 분기 가능
    @Value("${app.cookie.secure:true}")   // 운영은 true, 로컬 테스트시 false 설정 가능
    private boolean secure;

    @Value("${app.cookie.same-site:None}") // 크로스 도메인이면 None 권장
    private String sameSite;

    @Value("${app.cookie.path:/}")
    private String path;

    @Value("${app.cookie.domain:}") // 필요 시 .example.com 처럼 지정, 기본은 빈값(호스트 스코프)
    private String domain;

    public ResponseCookie create(String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge);

        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build();
    }

    public ResponseCookie createHttpOnly(String name, String value, Duration maxAge) {
        return create(name, value, maxAge, true);
    }

    public ResponseCookie createReadable(String name, String value, Duration maxAge) {
        // 비 HttpOnly 쿠키(XSRF-TOKEN 등)
        return create(name, value, maxAge, false);
    }

    public ResponseCookie delete(String name) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0);
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build();
    }

}
