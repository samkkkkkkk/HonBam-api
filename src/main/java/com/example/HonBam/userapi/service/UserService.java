package com.example.HonBam.userapi.service;

import com.example.HonBam.auth.CustomUserDetails;
import com.example.HonBam.auth.TokenProvider;
//import com.example.HonBam.aws.S3Service;
import com.example.HonBam.userapi.dto.request.LoginRequestDTO;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.userapi.dto.response.KakaoUserDTO;
import com.example.HonBam.userapi.dto.response.LoginResponseDTO;
import com.example.HonBam.userapi.dto.response.UserSignUpResponseDTO;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserPay;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.exception.DuplicateEmailException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.exception.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
//    final S3Service s3Service;

    @Value("${kakao.client_id}")
    private String KAKAO_CLIENT_ID;
    @Value("${kakao.redirect_url}")
    private String KAKAO_REDIRECT_URI;
    @Value("${kakao.client_secret}")
    private String KAKAO_CLIENT_SECRET;




    @Value("${upload.path}")
    private String uploadRootPath;

    // 회원 가입 처리
    public UserSignUpResponseDTO create(
            final UserRequestSignUpDTO dto,
            final String uploadedFilePath

    ) {
        String email = dto.getEmail();

        if(isDuplicate(email, "email")) {
            log.warn("이메일이 중복되었습니다. - {}", email);
            throw new DuplicateEmailException("중복된 이메일 입니다.");
        }

        // 패스워드 인코딩
        String encoded = passwordEncoder.encode(dto.getPassword());
        dto.setPassword(encoded);

        // dto를 User Entity로 변환해서 저장
        User saved = userRepository.save(dto.toEntity(uploadedFilePath));
        log.info("회원 가입 정상 수행됨! - saved user - {}", saved);

        return new UserSignUpResponseDTO(saved);

    }

    public boolean isDuplicate(String target, String value) {

        if (target.equals("userId")) {
            return userRepository.existsByUserId(value);
        } else if (target.equals("email")) {
            return userRepository.existsByEmail(value);
        }

        return false;
    }

    // 회원 인증
    public LoginResponseDTO authenticate(final LoginRequestDTO dto) {

        // 이메일을 통해 회원 정보 조회
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new UserNotFoundException("가입된 회원이 아닙니다.")
                );

        // 패스워드 검증
        String rawPassword = dto.getPassword(); // 입력한 비번
        String encodedPassword = user.getPassword(); // DB에 저장된 암호화된 비번

        if(!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidPasswordException("비밀번호가 틀렸습니다.");
        }

        log.info("{}님 로그인 성공!", user.getUserName());

        // 로그인 성공 후에 클라이언트에게 뭘 리턴할 것인가???
        // -> JWT를 클라이언트에게 발급해 주어야 한다!
        String token = tokenProvider.createToken(user);

        return new LoginResponseDTO(user, token);

    }


    // 프리미엄으로 등급 업
    public LoginResponseDTO promoteToPremium(CustomUserDetails userDetails) {

        User foundUser = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(
                        () -> new UserNotFoundException("회원 조회에 실패했습니다!")
                );

        // 일반(COMMON) 회원이 아니라면 예외 발생
//        if(userInfo.getRole() != Role.COMMON) {
//            throw new IllegalArgumentException("일반 회원이 아니라면 등급을 상승시킬 수 없습니다.");
//        }

        // 등급 변경
        foundUser.changeRole(Role.PREMIUM);
        User saved = userRepository.save(foundUser);

        // 토큰을 재발급! (새롭게 변경된 정보로)
        String token = tokenProvider.createToken(saved);

        return new LoginResponseDTO(saved, token);
    }

    // 프리미엄으로 등급 업 ( 결제 )
    public LoginResponseDTO promoteToPayPremium(CustomUserDetails userDetails) {

        User foundUser = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(
                        () -> new UserNotFoundException("회원 조회에 실패했습니다!")
                );

        // 일반(COMMON) 회원이 아니라면 예외 발생
//        if(userInfo.getRole() != Role.COMMON) {
//            throw new IllegalArgumentException("일반 회원이 아니라면 등급을 상승시킬 수 없습니다.");
//        }

        // 등급 변경
        foundUser.changeUserPay(UserPay.PREMIUM);
        User saved = userRepository.save(foundUser);

        // 토큰을 재발급! (새롭게 변경된 정보로)
        String token = tokenProvider.createToken(saved);

        return new LoginResponseDTO(saved, token);
    }

    /**
     * 업로드 된 파일을 서버에 저장하고 저장 경로를 리턴.
     *
     * @param profileImg - 업로드 된 파일의 정보
     * @return 실제로 저장된 이미지 경로
     */
    public String uploadProfileImage(MultipartFile profileImg) throws IOException {

//         루트 디렉토리가 실존하는 지 확인 후 존재하지 않으면 생성.
        File rootDir = new File(uploadRootPath);
        if(!rootDir.exists()) rootDir.mkdirs();

        // 파일명을 유니크하게 변경 (이름 충돌 가능성을 대비)
        // UUID와 원본파일명을 혼합. -> 규칙은 없어요.
        String uniqueFileName
                = UUID.randomUUID() + "_" + profileImg.getOriginalFilename();

//         파일을 저장
        File uploadFile = new File(uploadRootPath + "/" + uniqueFileName);
        profileImg.transferTo(uploadFile);

        return uniqueFileName;
//        return s3Service.uploadToS3Bucket(profileImg.getBytes(), uniqueFileName);
    }

    public String findProfilePath(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
//        return user.getProfileImg();
//         DB에 저장되는 profile_img는 파일명. -> service가 가지고 있는 Root Path와 연결해서 리턴.
        if(user.getAccessToken() != null){
            return user.getProfileImg();
        }
        return uploadRootPath + "/" + user.getProfileImg();


    }

    public LoginResponseDTO kakaoService(final String code) {

        // 인가코드를 통해 토큰 발급받기
        Map<String, Object> responseData = getKakaoAccessToken(code);
//        log.info("token: {}", responseData.get("access_token"));

        // 토큰을 통해 사용자 정보 가져오기
        KakaoUserDTO dto = getKakaoUserInfo((String)responseData.get("access_token"));

        // 일회성 로그인으로 처리 -> dto를 바로 화면단으로 리턴
        // 회원가입 처리 -> 이메일 중복 검사 진행 -> 자체 jwt를 생성해서 토큰을 화면단에 리턴.
        // -> 화면단에서는 적절한 url을 선택하여 redirect를 진행.

        if(!isDuplicate(dto.getKakaoAccount().getEmail(), "email")) {
            // 이메일이 중복되지 않았다 -> 이전에 로그인 한 적이 없음 -> DB에 데이터를 세팅
            User saved = userRepository.save(dto.toEntity((String)responseData.get("access_token")));
        }
        // 이메일이 중복됐다? -> 이전에 로그인 한 적이 있다. -> DB에 데이터를 또 넣을 필요는 없다.
        User foundUser = userRepository.findByEmail(dto.getKakaoAccount().getEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String token = tokenProvider.createToken(foundUser);

        foundUser.setAccessToken((String)responseData.get("access_token"));
        userRepository.save(foundUser);

        return new LoginResponseDTO(foundUser, token);

    }

    private KakaoUserDTO getKakaoUserInfo(String accessToken) {

        // 요청 uri
        String requestUri = "https://kapi.kakao.com/v2/user/me";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청 보내기
        RestTemplate template = new RestTemplate();
        ResponseEntity<KakaoUserDTO> responseEntity
                = template.exchange(requestUri, HttpMethod.GET, new HttpEntity<>(headers), KakaoUserDTO.class);

        // 응답 바디 읽기
        KakaoUserDTO responseData = responseEntity.getBody();
//        log.info("user profile: {}", responseData);

        return responseData;
    }

    private Map<String, Object> getKakaoAccessToken(String code) {

        // 요청 uri
        String requestUri = "https://kauth.kakao.com/oauth/token";

        //요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청 바디(파라미터) 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code"); // 카카오 공식 문서 기준 값으로 세팅
        params.add("client_id", KAKAO_CLIENT_ID); // 카카오 디벨로퍼 REST API 키
        params.add("redirect_uri", KAKAO_REDIRECT_URI); // 카카오 디벨로퍼 등록된 redirect uri
        params.add("code", code); // 프론트에서 인가 코드 요청시 전달받은 코드값
        params.add("client_secret", KAKAO_CLIENT_SECRET); // 카카오 디벨로퍼 client secret(활성화 시 추가해 줘야 함)

        // 헤더와 바디 정보를 합치기 위해 HttpEntity 객체 생성
        HttpEntity<Object> requestEntity = new HttpEntity<>(params, headers);

        // 카카오 서버로 POST 통신
        RestTemplate template = new RestTemplate();

        // 통신을 보내면서 응답데이터를 리턴
        // param1: 요청 url
        // param2: 요청 메서드 (전송 방식)
        // param3: 헤더와 요청 파라미터정보 엔터티
        // param4: 응답 데이터를 받을 객체의 타입 (ex: dto, map)
        // 만약 구조가 복잡한 경우에는 응답 데이터 타입을 String으로 받아서 JSON-simple 라이브러리로 직접 해체.
        ResponseEntity<Map> responseEntity
                = template.exchange(requestUri, HttpMethod.POST, requestEntity, Map.class);

        // 응답 데이터에서 필요한 정보를 가져오기
        Map<String, Object> responseData = (Map<String, Object>)responseEntity.getBody();
//        log.info("토큰 요청 응답 데이터: {}", responseData);

        return responseData;
    }

    public String logout(CustomUserDetails userDetails) {
        User foundUser = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String accessToken = foundUser.getAccessToken();
        if(accessToken != null) {
            String reqUri = "https://kapi.kakao.com/v1/user/logout";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);

            RestTemplate template = new RestTemplate();
            ResponseEntity<String> responseData
                    = template.exchange(reqUri, HttpMethod.POST, new HttpEntity<>(headers), String.class);
            return responseData.getBody();
        }
        return null;
    }





    // 사용자 삭제 함수
    public void delete(String userId) {
        // 데이터베이스에서 사용자 ID를 이용해 사용자를 찾습니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 사용자가 존재하면 삭제합니다.
        userRepository.delete(user);
    }
}


