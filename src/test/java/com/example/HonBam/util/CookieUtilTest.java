package com.example.HonBam.util;

import com.example.HonBam.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CookieUtil 단위 테스트 (Spring 컨텍스트 없이 AuthProperties를 직접 구성).
 */
class CookieUtilTest {

    private CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();

        AuthProperties.CookieSetting access = props.getCookie().getAccess();
        access.setMaxAgeMinutes(30);
        access.setHttpOnly(true);
        access.setSecure(true);
        access.setSameSite("None");
        access.setPath("/");

        AuthProperties.CookieSetting refresh = props.getCookie().getRefresh();
        refresh.setMaxAgeDays(14);
        refresh.setHttpOnly(true);
        refresh.setSecure(true);
        refresh.setSameSite("None");
        refresh.setPath("/");

        cookieUtil = new CookieUtil(props);
    }

    @Test
    @DisplayName("access 쿠키는 설정된 속성과 분 단위 만료를 가진다")
    void createAccessCookie() {
        ResponseCookie cookie = cookieUtil.createAccessCookie("access-value");

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("access-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("refresh 쿠키는 설정된 속성과 일 단위 만료를 가지며 원본 값을 그대로 담는다")
    void createRefreshCookie() {
        ResponseCookie cookie = cookieUtil.createRefreshCookie("raw-refresh-token");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(14));
    }

    @Test
    @DisplayName("삭제 쿠키는 maxAge=0으로 즉시 만료된다")
    void deleteCookies() {
        assertThat(cookieUtil.deleteAccessCookie().getMaxAge()).isZero();
        assertThat(cookieUtil.deleteRefreshCookie().getMaxAge()).isZero();
    }
}
