package com.example.HonBam.userapi.service;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.auth.entity.RefreshToken;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import com.example.HonBam.config.AuthProperties;
import com.example.HonBam.exception.DuplicateEmailException;
import com.example.HonBam.exception.InvalidPasswordException;
import com.example.HonBam.exception.InvalidRefreshTokenException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.userapi.dto.request.LoginRequestDTO;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.userapi.dto.response.*;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthProperties authProperties;

    @Value("${kakao.client_id}")
    private String KAKAO_CLIENT_ID;
    @Value("${kakao.redirect_url}")
    private String KAKAO_REDIRECT_URI;
    @Value("${kakao.client_secret}")
    private String KAKAO_CLIENT_SECRET;

    @Value("${upload.path}")
    private String uploadRootPath;

    private User findUserByToken(TokenUserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            throw new RuntimeException("인증 정보가 유효하지 않습니다.");
        }
        return userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new UserNotFoundException("회원 조회에 실패했습니다!"));
    }

    // 회원 가입 처리
    public UserSignUpResponseDTO create(
            final UserRequestSignUpDTO dto,
            final String uploadedFilePath
    ) {
        String email = dto.getEmail();

        if (isDuplicate(email, "email")) {
            log.warn("이메일이 중복되었습니다. - {}", email);
            throw new DuplicateEmailException("중복된 이메일 입니다.");
        }

        String encoded = passwordEncoder.encode(dto.getPassword());
        dto.setPassword(encoded);

        User saved = userRepository.save(dto.toEntity(uploadedFilePath));
        log.info("회원 가입 정상 수행됨! - saved user - {}", saved);

        return new UserSignUpResponseDTO(saved);
    }

    public boolean isDuplicate(String target, String value) {
        if (target.equals("userId")) {
            return userRepository.existsByNickname(value);
        } else if (target.equals("email")) {
            return userRepository.existsByEmail(value);
        }
        return false;
    }

    public User authenticate(final LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("가입된 회원이 아닙니다."));

        String rawPassword = dto.getPassword();
        String encodedPassword = user.getPassword();

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidPasswordException("비밀번호가 틀렸습니다.");
        }
        return user;
    }

    public LoginResponseDTO promoteToPayPremium(TokenUserInfo userInfo) {
        User foundUser = findUserByToken(userInfo);
        foundUser.changeRole(Role.PREMIUM);
        User saved = userRepository.save(foundUser);
        String token = tokenProvider.createAccessToken(saved);
        return new LoginResponseDTO(saved);
    }

    public String uploadProfileImage(MultipartFile profileImg) throws IOException {
        File rootDir = new File(uploadRootPath);
        if (!rootDir.exists()) rootDir.mkdirs();

        String uniqueFileName = UUID.randomUUID() + "_" + profileImg.getOriginalFilename();
        File uploadFile = new File(uploadRootPath + "/" + uniqueFileName);
        profileImg.transferTo(uploadFile);
        return uniqueFileName;
    }

    public String findProfilePath(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getAccessToken() != null) {
            return user.getProfileImg();
        }
        return uploadRootPath + "/" + user.getProfileImg();
    }

    public LoginResponseDTO kakaoService(final String code) {
        Map<String, Object> responseData = getKakaoAccessToken(code);
        KakaoUserDTO dto = getKakaoUserInfo((String) responseData.get("access_token"));

        if (!isDuplicate(dto.getKakaoAccount().getEmail(), "email")) {
            userRepository.save(dto.toEntity((String) responseData.get("access_token")));
        }
        User foundUser = userRepository.findByEmail(dto.getKakaoAccount().getEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String token = tokenProvider.createAccessToken(foundUser);

        String refresh = tokenProvider.createRefreshToken(foundUser);
        String refreshHash = tokenProvider.hashRefreshToken(refresh);

        // DB에 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(foundUser.getId())
                .tokenHash(refreshHash)
                .revoked(false)
                .expiredAt(LocalDateTime.now().plus(authProperties.getToken().getRefreshExpireDuration()))
                .deviceInfo("kakao-login")
                .build();
        refreshTokenRepository.save(refreshToken);

        // Redis 저장
        redisTemplate.opsForValue()
                .set("refresh:" + refreshHash, foundUser.getId(), authProperties.getToken().getRefreshExpireDuration());

        userRepository.save(foundUser);
        return new LoginResponseDTO(foundUser);
    }

    private KakaoUserDTO getKakaoUserInfo(String accessToken) {
        String requestUri = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RestTemplate template = new RestTemplate();
        ResponseEntity<KakaoUserDTO> responseEntity = template.exchange(requestUri, HttpMethod.GET, new HttpEntity<>(headers), KakaoUserDTO.class);
        return responseEntity.getBody();
    }

    private Map<String, Object> getKakaoAccessToken(String code) {
        String requestUri = "https://kauth.kakao.com/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", KAKAO_CLIENT_ID);
        params.add("redirect_uri", KAKAO_REDIRECT_URI);
        params.add("code", code);
        params.add("client_secret", KAKAO_CLIENT_SECRET);

        HttpEntity<Object> requestEntity = new HttpEntity<>(params, headers);
        RestTemplate template = new RestTemplate();
        ResponseEntity<Map> responseEntity = template.exchange(requestUri, HttpMethod.POST, requestEntity, Map.class);
        return (Map<String, Object>) responseEntity.getBody();
    }


    public void delete(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
    }

    public UserInfoResponseDTO getUserInfo(TokenUserInfo userInfo) {
        User user = findUserByToken(userInfo);
        return new UserInfoResponseDTO(user);
    }

    // refreshToken 발급
    @Transactional
    public RefreshResponseDTO refreshToken(String refresh) {

        String refreshHash = tokenProvider.hashRefreshToken(refresh);
        String redisKey = "refresh:" + refreshHash;

        // Redis 조회
        boolean redisFail = false;
        String userId = null;

        try {
            Object userIdObj = redisTemplate.opsForValue().get(redisKey);
            if (userIdObj != null) {
                userId = userIdObj.toString();
            }
        } catch (Exception e) {
            redisFail = true;
            log.warn("Redis 서버 연동 실패 {}", e.getMessage());
        }

        if (!redisFail && userId == null) {
            throw new InvalidRefreshTokenException("INVALID");
        }

        // DB 검증
        RefreshToken rt = refreshTokenRepository.findByTokenHash(refreshHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("REVOKED"));
        if (rt.isRevoked()) {
            throw new InvalidRefreshTokenException("REVOKED");
        }

        if (rt.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("EXPIRED");
        }

        if (userId == null) {
            userId = rt.getUserId();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 기존 refreshToken 사용 완료 처리
        rt.revoke();

        if (!redisFail) {
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                log.warn("Redis 삭제 실패 {}", e.getMessage());
            }
        }
        // 새로운 access token 발급
        String newAccess = tokenProvider.createAccessToken(user);


        String refreshToken = tokenProvider.createRefreshToken(user);
        String newRefreshHash = tokenProvider.hashRefreshToken(refreshToken);
        String newRedisKey = "refresh:" + newRefreshHash;

        // DB에 새 refresh 저장
        RefreshToken newRefresh = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(newRefreshHash)
                .revoked(false)
                .expiredAt(LocalDateTime.now().plus(authProperties.getToken().getRefreshExpireDuration()))
                .deviceInfo("local-login")
                .build();
        refreshTokenRepository.save(newRefresh);

        try {
            redisTemplate.opsForValue()
                    .set(newRedisKey, userId, authProperties.getToken().getRefreshExpireDuration());
            log.info("newRedisKey 저장 완료");
        } catch (Exception e) {
            log.warn("newRedisKey 저장 실패 {}", e.getMessage());
        }

        return new RefreshResponseDTO(newAccess, refreshToken);

    }
}


