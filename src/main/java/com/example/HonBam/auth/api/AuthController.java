package com.example.HonBam.auth.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.auth.dto.response.LoginResult;
import com.example.HonBam.auth.service.AuthService;
import com.example.HonBam.auth.dto.request.LoginRequestDTO;
import com.example.HonBam.auth.dto.response.RefreshResponseDTO;
import com.example.HonBam.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    // 로그인 요청
    @PostMapping("/login")
    public ResponseEntity<?> signIn(@Validated @RequestBody LoginRequestDTO dto) {

        LoginResult result = authService.signIn(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessCookie(result.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshCookie(result.getRefreshToken()).toString())
                .build();
    }

    // 로그아웃 요청
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            HttpServletResponse response) {
        log.info("로그아웃 요청이 들어옴");

        // Refresh Token 전부 폐기
        authService.invalidateUserTokens(userInfo.getUserId());

        // 쿠기 제거
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());

        return ResponseEntity.ok(Map.of("success", true));
    }

    // 토큰 재발급 요청
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token", required = false) String refresh) {
        if (refresh == null) {
            return ResponseEntity.status(401).body(Map.of("message","NOT_REFRESH_TOKEN"));
        }
        RefreshResponseDTO dto = authService.refreshToken(refresh);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createAccessCookie(dto.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createRefreshCookie(dto.getRefreshToken()).toString())
                .build();
    }


    // 카카오 로그인 요청
    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code) {
        LoginResult result = authService.kakaoService(code);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessCookie(result.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshCookie(result.getRefreshToken()).toString())
                .body(Map.of("success", true));
    }

    // 로그인 유효 검사
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal TokenUserInfo userInfo) {
        Boolean authenticated = userInfo != null;
        return ResponseEntity.ok().body(Map.of("authenticated", authenticated));
    }
}
