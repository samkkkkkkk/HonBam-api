package com.example.HonBam.filter;

import com.example.HonBam.exception.JwtAuthException;
import com.example.HonBam.exception.JwtErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 예외가 발생하지 않으면 Auth Filter로 통과
            filterChain.doFilter(request,response);
        } catch (JwtException e) {
            // Auth Filter에서 발생한 JWT 예외를 사유 코드로 매핑해 JSON 응답
            log.info("JWT 인증 예외 발생 ! - {}", e.getMessage());

            JwtErrorCode errorCode = (e instanceof JwtAuthException)
                    ? ((JwtAuthException) e).getErrorCode()
                    : JwtErrorCode.INVALID_JWT;

            setErrorResponse(response, errorCode.getHttpStatus(), errorCode.getCode());
        }
    }

    private void setErrorResponse(HttpServletResponse response,
                                  int status,
                                  String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");

        // Map 생성 및 데이터 추가
        Map<String,Object> responseMap = new HashMap<>();
        responseMap.put("message", message);
        responseMap.put("code", status);

        // json 데이터를 응답객체에 실어서 브라우저로 바로 응답.
        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}
