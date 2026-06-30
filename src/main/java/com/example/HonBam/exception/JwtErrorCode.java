package com.example.HonBam.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 인증 실패 사유를 타입으로 표현한다.
 * 각 값은 응답에 사용할 HTTP 상태코드와 클라이언트 약속 코드 문자열을 가진다.
 */
@Getter
@RequiredArgsConstructor
public enum JwtErrorCode {

    ACCESS_TOKEN_EXPIRED(401, "ACCESS_TOKEN_EXPIRED"),
    INVALID_TOKEN_TYPE(400, "INVALID_TOKEN_TYPE"),
    INVALID_REFRESH_TOKEN(400, "INVALID_REFRESH_TOKEN"),
    INVALID_JWT(401, "INVALID_JWT");

    private final int httpStatus;
    private final String code;
}
