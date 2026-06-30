package com.example.HonBam.exception;

import io.jsonwebtoken.JwtException;
import lombok.Getter;

/**
 * JWT 검증 실패 사유({@link JwtErrorCode})를 운반하는 예외.
 * {@link JwtException}을 상속하므로 기존 필터 체인(JwtAuthFilter → JwtExceptionFilter)의
 * {@code catch (JwtException)} 처리에 그대로 흡수된다.
 */
@Getter
public class JwtAuthException extends JwtException {

    private final JwtErrorCode errorCode;

    public JwtAuthException(JwtErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }
}
