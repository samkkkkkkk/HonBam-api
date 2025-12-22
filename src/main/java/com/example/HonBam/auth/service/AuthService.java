package com.example.HonBam.auth.service;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.dto.response.LoginResult;
import com.example.HonBam.auth.entity.RefreshToken;
import com.example.HonBam.auth.repository.RefreshTokenRepository;
import com.example.HonBam.config.AuthProperties;
import com.example.HonBam.exception.InvalidPasswordException;
import com.example.HonBam.exception.InvalidRefreshTokenException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.auth.dto.request.LoginRequestDTO;
import com.example.HonBam.auth.dto.response.KakaoUserDTO;
import com.example.HonBam.auth.dto.response.RefreshResponseDTO;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserProfileMedia;
import com.example.HonBam.userapi.repository.UserProfileMediaRepository;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${kakao.client_id}")
    private String KAKAO_CLIENT_ID;
    @Value("${kakao.redirect_url}")
    private String KAKAO_REDIRECT_URI;
    @Value("${kakao.client_secret}")
    private String KAKAO_CLIENT_SECRET;

    private final AuthProperties authProperties;
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MediaRepository mediaRepository;
    private final UserProfileMediaRepository userProfileMediaRepository;



    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("가입된 회원이 아닙니다."));

        String encodedPassword = user.getPassword();

        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new InvalidPasswordException("비밀번호가 틀렸습니다.");
        }
        return user;
    }

    public LoginResult signIn(final LoginRequestDTO dto) {
        User user = authenticate(dto.getEmail(), dto.getPassword());

        String accessToken = tokenProvider.createAccessToken(user);
        String refresh = tokenProvider.createRefreshToken(user);
        String refreshHash = tokenProvider.hashRefreshToken(refresh);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .revoked(false)
                .expiredAt(LocalDateTime.now().plusDays(authProperties.getToken().getRefreshExpireDays()))
                .build();

        refreshTokenRepository.save(refreshToken);

        redisTemplate.opsForValue()
                .set("refresh:" + refreshHash, user.getId(), authProperties.getToken().getRefreshExpireDuration());

        return new LoginResult(accessToken, refresh);

    }


    @Transactional
    public LoginResult kakaoService(final String code) {
        Map<String, Object> responseData = getKakaoAccessToken(code);
        String kakaoAccessToken = (String) responseData.get("access_token");


        KakaoUserDTO dto = getKakaoUserInfo(kakaoAccessToken);
        String email = dto.getKakaoAccount().getEmail();

        boolean exists = userRepository.existsByEmail(email);
        if (!exists) {
            User user = userRepository.save(dto.toEntity(kakaoAccessToken));

            // 카카오 프로필 이미지 URL 추출
            String kakaoProfileUrl = dto.getKakaoAccount().getProfile().getProfileImageUrl();

            if (kakaoProfileUrl != null && !kakaoProfileUrl.isBlank()) {
                saveKakaoProfileImage(user, kakaoProfileUrl);
            }
        }

        User foundUser = userRepository.findByEmail(dto.getKakaoAccount().getEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String accessToken = tokenProvider.createAccessToken(foundUser);

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

        return new LoginResult(accessToken, refresh);
    }

    private void saveKakaoProfileImage(User user, String kakaoProfileUrl) {
        try {
            // 1. Media 엔티티 생성 (S3 관련 로직 생략하고 URL만 저장)
            Media media = Media.builder()
                    .fileKey(kakaoProfileUrl) // 키 대신 전체 URL 저장
                    .contentType("IMAGE")       // 카카오는 보통 이미지
                    .mediaPurpose(MediaPurpose.PROFILE)
                    .build();

            // 2. Media 저장
            Media savedMedia = mediaRepository.save(media);

            // 3. UserProfileMedia 연결
            UserProfileMedia userProfileMedia = UserProfileMedia.builder()
                    .user(user)
                    .media(savedMedia)
                    .build();

            userProfileMediaRepository.save(userProfileMedia);

        } catch (Exception e) {
            log.warn("카카오 프로필 이미지 저장 실패 (로그인은 진행): {}", e.getMessage());
        }
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

    public boolean isDuplicate(String target, String value) {
        if (target.equals("userId")) {
            return userRepository.existsByNickname(value);
        } else if (target.equals("email")) {
            return userRepository.existsByEmail(value);
        }
        return false;
    }

    @Transactional
    public void invalidateUserTokens(String userId) {
        // DB에서 RefreshToken 목록 조회
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);

        // redis에 저장된 refresh:{hash} 삭제
        for (RefreshToken token : tokens) {
            String redisKey = "refresh:" + token.getTokenHash();
            redisTemplate.delete(redisKey);
            log.info("Invalidated redis refresh token: {}", redisKey);

            token.revoke();
        }

        // DB RefreshToken 삭제
        refreshTokenRepository.saveAll(tokens);
        log.info("DB refresh tokens removed for userId: {}", userId);
    }

    public void loginFromKakao(String accessToken) {

        WebClient webClient = WebClient.create();
        String url = "https://kapi.kakao.com/v1/user/logout";

        try {
            String response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("카카오 로그아웃 응답: {}", response);
        } catch (Exception e) {
            log.info("카카오 로그아웃 실패", e);
        }

    }

    // 네이버 로그아웃(리다이렉트 방식)
    public String getNaverLogout(String redirectUri) {
        log.info("네이버로그아웃");
        return "https://nid.naver.com/nidlogin.logout?returl=" + redirectUri;
    }


}
