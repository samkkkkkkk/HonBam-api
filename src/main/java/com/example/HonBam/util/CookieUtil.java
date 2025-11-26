package com.example.HonBam.util;

import com.example.HonBam.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    private final AuthProperties authProperties;

    public ResponseCookie createAccessCookie(String value) {
        AuthProperties.CookieSetting access = authProperties.getCookie().getAccess();

        Duration duration =
                (access.getMaxAgeMinutes() != null)
                        ? Duration.ofMinutes(access.getMaxAgeMinutes())
                        : Duration.ZERO;

        return ResponseCookie.from("access_token", value)
                .httpOnly(access.isHttpOnly())
                .secure(access.isSecure())
                .sameSite(access.getSameSite())
                .path(access.getPath())
                .maxAge(duration)
                .build();
    }


    public ResponseCookie createRefreshCookie(String token) {
        AuthProperties.CookieSetting refresh = authProperties.getCookie().getRefresh();
        Duration duration =
                (refresh.getMaxAgeDays() != null)
                        ? Duration.ofDays(refresh.getMaxAgeDays())
                        : Duration.ZERO;

        return ResponseCookie.from("refresh_token", token)
                .maxAge(duration)
                .httpOnly(refresh.isHttpOnly())
                .secure(refresh.isSecure())
                .sameSite(refresh.getSameSite())
                .path(refresh.getPath())
                .build();
    }

    public ResponseCookie deleteAccessCookie() {
        AuthProperties.CookieSetting access = authProperties.getCookie().getAccess();

        return ResponseCookie.from("access_token", "")
                .httpOnly(access.isHttpOnly())
                .secure(access.isSecure())
                .sameSite(access.getSameSite())
                .path(access.getPath())
                .maxAge(0)
                .build();
    }

    public ResponseCookie deleteRefreshCookie() {
        AuthProperties.CookieSetting refresh = authProperties.getCookie().getRefresh();

        return ResponseCookie.from("refresh_token", "")
                .httpOnly(refresh.isHttpOnly())
                .secure(refresh.isSecure())
                .sameSite(refresh.getSameSite())
                .path(refresh.getPath())
                .maxAge(0)
                .build();
    }

}
