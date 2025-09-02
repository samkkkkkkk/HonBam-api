package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;

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

        // 2. httpOnly 쿠키로 주입( 컨트롤러와 동일한 정챌 15분/14일)
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createHttpOnly("access_token", access, Duration.ofMinutes(15)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createHttpOnly("refresh_token", refresh, Duration.ofDays(14)).toString());

        // 3. 프론트로 리다이렉트
        response.sendRedirect(successRedirectUrl);
    }
}
