package com.example.HonBam.userapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.exception.NoRegisteredArgumentsException;
import com.example.HonBam.userapi.dto.request.LoginRequestDTO;
import com.example.HonBam.userapi.dto.request.UserRequestSignUpDTO;
import com.example.HonBam.userapi.dto.response.LoginResponseDTO;
import com.example.HonBam.userapi.dto.response.UserSignUpResponseDTO;
import com.example.HonBam.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")

public class UserController {

    private final UserService userService;

    // 이메일 중복 확인 요청 처리
    // GET: /api/auth/check?email=zzzz@xxx.com
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam(value = "value") String value,
                                   @RequestParam(value = "target") String target
    ) {
        if(target.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("이메일이 없습니다!");
        }

        boolean resultFlag = userService.isDuplicate(target, value);
        log.info("{} 중복?? - {}", target, resultFlag);

        return ResponseEntity.ok().body(resultFlag);
    }

    // 회원 가입 요청 처리
    // POST: /api/auth
    @PostMapping
    public ResponseEntity<?> signUp(
            @Validated @RequestPart("user") UserRequestSignUpDTO dto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImg,
            BindingResult result
    ) {
        log.info("/api/auth POST! - {}", dto);


        if(result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {
            String uploadedFilePath = null;
            if(profileImg != null) {
                log.info("attached file name: {}", profileImg.getOriginalFilename());
                // 전달받은 프로필 이미지를 먼저 지정된 경로에 저장한 후 DB 저장을 위해 경로를 받아오자.
                uploadedFilePath = userService.uploadProfileImage(profileImg);
            }

            UserSignUpResponseDTO responseDTO = userService.create(dto, uploadedFilePath);
            return ResponseEntity.ok()
                    .body(responseDTO);
        } catch (RuntimeException e) {
            log.warn("이메일 중복!");
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.warn("기타 예외가 발생했습니다!");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 로그인 요청 처리
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(
            @Validated @RequestBody LoginRequestDTO dto
    ) {
        try {
            LoginResponseDTO responseDTO
                    = userService.authenticate(dto);

            return ResponseEntity.ok().body(responseDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // 일반 회원을 프리미엄 회원으로 승격하는 요청 처리
    @PutMapping("/promote")
    // 권한 검사 (해당 권한이 아니라면 인가처리 거부 -> 403 코드 리턴)
    // 메서드 호출 전에 권한 검사 -> 요청 당시 토큰에 있는 user 정보가 ROLE_COMMON이라는 권한을 가지고 있는지 검사.
    @PreAuthorize("hasRole('ROLE_COMMON')")
    public ResponseEntity<?> promote(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/auth/promote PUT!");

        try {
            LoginResponseDTO responseDTO = userService.promoteToPremium(userInfo);
            return ResponseEntity.ok()
                    .body(responseDTO);
        } catch (NoRegisteredArgumentsException | IllegalArgumentException e) {
            // 예상 가능한 예외 (직접 생성하는 예외 처리)
            e.printStackTrace();
            log.warn(e.getMessage());
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            // 예상하지 못한 예외 처리
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }


    // 일반 회원을 프리미엄 회원으로 승격하는 요청 처리
    @PutMapping("/paypromote")
    // 권한 검사 (해당 권한이 아니라면 인가처리 거부 -> 403 코드 리턴)
    // 메서드 호출 전에 권한 검사 -> 요청 당시 토큰에 있는 user 정보가 ROLE_COMMON이라는 권한을 가지고 있는지 검사.
    @PreAuthorize("hasRole('ROLE_NORMAL')")
    public ResponseEntity<?> paypromote(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/auth/paypromote PUT!");

        try {
            LoginResponseDTO responseDTO = userService.promoteToPayPremium(userInfo);
            return ResponseEntity.ok()
                    .body(responseDTO);
        } catch (NoRegisteredArgumentsException | IllegalArgumentException e) {
            // 예상 가능한 예외 (직접 생성하는 예외 처리)
            e.printStackTrace();
            log.warn(e.getMessage());
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            // 예상하지 못한 예외 처리
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }

    // 프로필 사진 이미지 데이터를 클라이언트에게 응답 처리
    @GetMapping("/load-profile")
    public ResponseEntity<?> loadFile(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/auth/load-profile - GET!, user: {}", userInfo.getEmail());

        try {
            // 클라이언트가 요청한 프로필 사진을 응답해야 함.
            // 1. 프로필 사진의 경로부터 얻어야 한다!
            String filePath
                    = userService.findProfilePath(userInfo.getUserId());

            // 2. 얻어낸 파일 경로를 통해 실제 파일 데이터를 로드하기.
            File profileFile = new File(filePath);

            // 모든 사용자가 프로필 사진을 가지는 것은 아니다. -> 프사가 없는 사람들은 경로가 존재하지 않을 것이다.
            // 만약 존재하지 않는 경로라면 클라이언트로 404 status를 리턴.
            if(!profileFile.exists()) {
                if(filePath.startsWith("http://")) {
                    return ResponseEntity.ok().body(filePath);
                }
                return ResponseEntity.notFound().build();
            }

            // 해당 경로에 저장된 파일을 바이트 배열로 직렬화 해서 리턴.
            byte[] fileData = FileCopyUtils.copyToByteArray(profileFile);

            // 3. 응답 헤더에 컨텐츠 타입을 설정.
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = findExtensionAndGetMediaType(filePath);
            if(contentType == null) {
                return ResponseEntity.internalServerError()
                        .body("발견된 파일은 이미지 파일이 아닙니다.");
            }
            headers.setContentType(contentType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("파일을 찾을 수 없습니다.");
        }
    }

    private MediaType findExtensionAndGetMediaType(String filePath) {

        // 파일 경로에서 확장자 추출하기
        // C:/HonBam_upload/nsadjknjkncjndnjs_abc.jpg
        String ext
                = filePath.substring(filePath.lastIndexOf(".") + 1);

        // 추출한 확장자를 바탕으로 MediaType을 설정. -> Header에 들어갈 Content-type이 됨.
        switch (ext.toUpperCase()) {
            case "JPG": case "JPEG":
                return MediaType.IMAGE_JPEG;
            case "PNG":
                return MediaType.IMAGE_PNG;
            case "GIF":
                return MediaType.IMAGE_GIF;
            default:
                return null;
        }
    }

    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(String code) {
//        log.info("/api/auth/kakaoLogin - GET! -code: {}", code);
        LoginResponseDTO responseDTO = userService.kakaoService(code);

        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping("/naverLogin")
    public ResponseEntity<?> naverLogin(@RequestParam("code") String code) {
        log.info("/api/auth/naverLogin - GET! -code: {}", code);
        LoginResponseDTO responseDTO = userService.NaverService(code);

        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping("/googleLogin")
    public ResponseEntity<?> googleLogin(@RequestParam("code") String code) {
        log.info("/api/auth/naverLogin - GET! -code: {}", code);
        LoginResponseDTO responseDTO = userService.GoogleService(code);

        return ResponseEntity.ok().body(responseDTO);
    }

    // 로그아웃 처리
    @GetMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/auth/logout - GET! - user: {}", userInfo.getUserId());

        String result = userService.logout(userInfo);
        return ResponseEntity.ok().body(result);
    }


    // 회원 탈퇴 요청 처리
// DELETE: /api/auth
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("/api/auth DELETE! - user: {}", userInfo.getEmail());

        try {
            // TokenUserInfo에서 userId를 추출합니다.
            String userId = userInfo.getUserId();

            // userId를 이용하여 해당 사용자를 삭제합니다.
            userService.delete(userId);

            return ResponseEntity.ok().body("회원 탈퇴가 정상적으로 처리되었습니다. " + userInfo.getEmail() + "님, 서비스를 이용해 주셔서 감사합니다.");
        } catch (Exception e) {
            log.warn(userInfo.getEmail() + "님의 회원 탈퇴 처리 중 에러가 발생했습니다!");
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(userInfo.getEmail() + "님의 회원 탈퇴 처리 중 문제가 발생했습니다. 다시 시도해 주세요.");
        }
    }




}