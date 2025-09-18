package com.example.HonBam.userapi.api;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import com.example.HonBam.exception.NoRegisteredArgumentsException;
import com.example.HonBam.userapi.entity.LoginProvider;
import com.example.HonBam.userapi.service.SocialLogoutService;
import com.example.HonBam.util.CookieUtil;
import com.example.HonBam.userapi.dto.request.LoginRequestDTO;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.userapi.dto.response.LoginResponseDTO;
import com.example.HonBam.userapi.dto.response.UserInfoResponseDTO;
import com.example.HonBam.userapi.dto.response.UserSignUpResponseDTO;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final SocialLogoutService socialLogoutService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam(value = "value") String value, @RequestParam(value = "target") String target) {
        if (target.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이메일이 없습니다!");
        }
        boolean resultFlag = userService.isDuplicate(target, value);
        log.info("{} 중복?? - {}", target, resultFlag);
        return ResponseEntity.ok().body(resultFlag);
    }

    @PostMapping
    public ResponseEntity<?> signUp(@Validated @RequestPart("user") UserRequestSignUpDTO dto, @RequestPart(value = "profileImage", required = false) MultipartFile profileImg, BindingResult result) {
        log.info("/api/auth POST! - {}", dto);
        if (result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest().body(result.getFieldError());
        }
        try {
            String uploadedFilePath = null;
            if (profileImg != null) {
                log.info("attached file name: {}", profileImg.getOriginalFilename());
                uploadedFilePath = userService.uploadProfileImage(profileImg);
            }
            UserSignUpResponseDTO responseDTO = userService.create(dto, uploadedFilePath);
            return ResponseEntity.ok().body(responseDTO);
        } catch (RuntimeException e) {
            log.warn("이메일 중복!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.warn("기타 예외가 발생했습니다!");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> signIn(@Validated @RequestBody LoginRequestDTO dto) {
        User user = userService.authenticate(dto);
        String access = tokenProvider.createAccessToken(user);
        String refresh = tokenProvider.createRefreshToken(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createHttpOnly("access_token", access, Duration.ofMinutes(15)).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createHttpOnly("refresh_token", refresh, Duration.ofDays(14)).toString())
                .body(new LoginResponseDTO(user));
    }

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

    @GetMapping("/profile-image")
    public ResponseEntity<?> loadFile(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth/load-profile - GET!, user: {}", userInfo.getUserId());
        try {
            String filePath = userService.findProfilePath(userInfo.getUserId());
            File profileFile = new File(filePath);
            if (!profileFile.exists()) {
                if (filePath.startsWith("http://")) {
                    return ResponseEntity.ok().body(filePath);
                }
                return ResponseEntity.notFound().build();
            }
            byte[] fileData = FileCopyUtils.copyToByteArray(profileFile);
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = findExtensionAndGetMediaType(filePath);
            if (contentType == null) {
                return ResponseEntity.internalServerError().body("발견된 파일은 이미지 파일이 아닙니다.");
            }
            headers.setContentType(contentType);
            return ResponseEntity.ok().headers(headers).body(fileData);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("파일을 찾을 수 없습니다.");
        }
    }

    private MediaType findExtensionAndGetMediaType(String filePath) {
        String ext = filePath.substring(filePath.lastIndexOf(".") + 1);
        switch (ext.toUpperCase()) {
            case "JPG": case "JPEG": return MediaType.IMAGE_JPEG;
            case "PNG": return MediaType.IMAGE_PNG;
            case "GIF": return MediaType.IMAGE_GIF;
            default: return null;
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal TokenUserInfo userInfo) {
        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of("message", "UNAUTHORIZED"));
        }
        User user = userRepository.findById(userInfo.getUserId()).orElseThrow();
        UserInfoResponseDTO dto = new UserInfoResponseDTO(user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(String code) {
        LoginResponseDTO responseDTO = userService.kakaoService(code);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, Authentication authentication) {
        log.info("로그아웃 요청이 들어옴");
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.delete("access_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.delete("refresh_token").toString());

        TokenUserInfo principal = (TokenUserInfo) authentication.getPrincipal();
        socialLogoutService.invalidateUserTokens(principal.getUserId());

        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        if (user.getLoginProvider() == LoginProvider.KAKAO) {
            String key = "social:token:" + user.getId();
            Object accessTokenObj = redisTemplate.opsForHash().get(key, "accessToken");
            if (accessTokenObj != null) {
                socialLogoutService.loginFromKakao(accessTokenObj.toString());
            }
            redisTemplate.delete(key);
        } else if (user.getLoginProvider() == LoginProvider.NAVER) {
            String naverLoginUrl = socialLogoutService.getNaverLogout("http://localhost:3000");
            return ResponseEntity.ok(Map.of("redirectUrl", naverLoginUrl));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "logout success"));
    }

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

    @GetMapping("/profile-s3")
    public ResponseEntity<?> s3Profile(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            String profilePath = userService.findProfilePath(userInfo.getUserId());
            return ResponseEntity.ok().body(profilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/userinfo")
    public ResponseEntity<?> userInfo(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("userinfo 요청!");
        UserInfoResponseDTO responseDto = userService.getUserInfo(userInfo);
        return ResponseEntity.ok().body(responseDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req) {
        String r = extractCookie(req,"refresh_token");
        if (r == null || !tokenProvider.validateRefreshToken(r)) return ResponseEntity.status(401).build();
        var info = tokenProvider.parseRefreshToken(r);
        String newAccess = tokenProvider.createAccessTokenByInfo(info);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createHttpOnly("access_token", newAccess, Duration.ofMinutes(15)).toString())
                .build();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        var cs = req.getCookies(); if (cs == null) return null;
        for (var c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}