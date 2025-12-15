package com.example.HonBam.userapi.api;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.auth.entity.RefreshToken;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import com.example.HonBam.config.AuthProperties;
import com.example.HonBam.exception.NoRegisteredArgumentsException;
import com.example.HonBam.userapi.dto.request.LoginRequestDTO;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.userapi.dto.response.LoginResponseDTO;
import com.example.HonBam.userapi.dto.response.RefreshResponseDTO;
import com.example.HonBam.userapi.dto.response.UserInfoResponseDTO;
import com.example.HonBam.userapi.dto.response.UserSignUpResponseDTO;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.userapi.service.SocialLogoutService;
import com.example.HonBam.userapi.service.UserService;
import com.example.HonBam.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final AuthProperties authProperties;
    private final CookieUtil cookieUtil;
    private final SocialLogoutService socialLogoutService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    // 이메일 중복 체크
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam(value = "value") String value, @RequestParam(value = "target") String target) {
        if (target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이메일이 없습니다!");
        }
        boolean resultFlag = userService.isDuplicate(target, value);
        log.info("{} 중복?? - {}", target, resultFlag);
        return ResponseEntity.ok().body(resultFlag);
    }

    // 회원가입
    @PostMapping
    public ResponseEntity<?> signUp(@Validated @RequestBody UserRequestSignUpDTO dto,  BindingResult result) {
        log.info("/api/auth POST! - {}", dto);
        if (result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest().body(result.getFieldError());
        }
        try {
            UserSignUpResponseDTO responseDTO = userService.create(dto);
            return ResponseEntity.ok().body(responseDTO);
        } catch (RuntimeException e) {
            log.warn("이메일 중복!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.warn("기타 예외가 발생했습니다!");
            return ResponseEntity.internalServerError().build();
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> signIn(@Validated @RequestBody LoginRequestDTO dto) {
        User user = userService.authenticate(dto);

        String access = tokenProvider.createAccessToken(user);
        String refresh = tokenProvider.createRefreshToken(user);
        String refreshHash = tokenProvider.hashRefreshToken(refresh);

        // DB 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .revoked(false)
                .expiredAt(LocalDateTime.now().plusDays(authProperties.getToken().getRefreshExpireDays()))
                .deviceInfo("local-login")
                .build();
        refreshTokenRepository.save(refreshToken);

        // Redis 저장
        redisTemplate.opsForValue()
                .set("refresh:" + refreshHash, user.getId(), authProperties.getToken().getRefreshExpireDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessCookie(access).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshCookie(refresh).toString())
                .body(new LoginResponseDTO(user));
    }

    // 외원 등급 승급
    @PutMapping("/paypromote")
    @PreAuthorize("hasRole('ROLE_COMMON')")
    public ResponseEntity<?> paypromote(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth/paypromote PUT!");
        try {
            LoginResponseDTO responseDTO = userService.promoteToPayPremium(userInfo);
            return ResponseEntity.ok().body(responseDTO);
        } catch (NoRegisteredArgumentsException | IllegalArgumentException e) {
            e.printStackTrace();
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // 프로필 이미지 요청
    @GetMapping("/profile-image")
    public ResponseEntity<?> loadFile(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth/load-profile - GET!, user: {}", userInfo.getUserId());
        try {
            // 서비스에서 URL 가져오기
            String profileUrl = userService.getProfileUrl(userInfo.getUserId());

            // 프로필 이미지가 없는 경우 처리 (선택 사항)
            if (profileUrl == null) {
                // 프론트엔드와 약속된 기본 이미지 URL을 주거나, null을 리턴
                return ResponseEntity.ok().body(Map.of("profileUrl", ""));
            }

            // JSON 형태로 URL 반환: { "profileUrl": "https://s3..." }
            return ResponseEntity.ok().body(Map.of("profileUrl", profileUrl));

        } catch (Exception e) {
            log.error("프로필 URL 조회 실패", e);
            return ResponseEntity.internalServerError().body("프로필 정보를 불러오는데 실패했습니다.");
        }
    }



    // 로그인 유효 검사
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal TokenUserInfo userInfo) {
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of("message", "UNAUTHORIZED"));
        }
        User user = userRepository.findById(userInfo.getUserId()).orElseThrow();
        UserInfoResponseDTO dto = new UserInfoResponseDTO(user);
        return ResponseEntity.ok(dto);
    }

    // 카카오 로그인 요청
    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(String code) {
        LoginResponseDTO responseDTO = userService.kakaoService(code);
        return ResponseEntity.ok().body(responseDTO);
    }

    // 로그아웃 요청
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            HttpServletResponse response) {
        log.info("로그아웃 요청이 들어옴");

        // Refresh Token 전부 폐기
        socialLogoutService.invalidateUserTokens(userInfo.getUserId());

        // 쿠기 제거
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());

        return ResponseEntity.ok(Map.of("success", true));
    }

    // 회원 탈퇴 요청
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth DELETE! - user: {}", userInfo.getUserId());
        try {
            userService.delete(userInfo.getUserId());
            return ResponseEntity.ok().body("회원 탈퇴가 정상적으로 처리되었습니다. " + userInfo.getUserId() + "님, 서비스를 이용해 주셔서 감사합니다.");
        } catch (Exception e) {
            log.warn(userInfo.getUserId() + "님의 회원 탈퇴 처리 중 에러가 발생했습니다!");
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(userInfo.getUserId() + "님의 회원 탈퇴 처리 중 문제가 발생했습니다. 다시 시도해 주세요.");
        }
    }

    // 유저 정보 요청
    @GetMapping("/userinfo")
    public ResponseEntity<?> userInfo(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("userinfo 요청!");
        UserInfoResponseDTO responseDto = userService.getUserInfo(userInfo);
        return ResponseEntity.ok().body(responseDto);
    }

    // refresh token 재발급 요청
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req) {

        String refresh = extractCookie(req, "refresh_token");
        if (refresh == null) {
            return ResponseEntity.status(401).body("NOT_REFRESH_TOKEN");
        }

        RefreshResponseDTO dto = userService.refreshToken(refresh);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createAccessCookie(dto.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createRefreshCookie(dto.getRefreshToken()).toString())
                .build();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}