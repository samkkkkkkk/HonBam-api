package com.example.HonBam.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            //토큰이 만료 되었을 시 Auth Filter에서 강제 예외 발생 -> 앞에있는 Exception filter로 전달
            log.info("만료 예외 발생 ! - {}" , e.getMessage());

            //  Access Token 만료
            if (e.getMessage().contains("expired")) {
                setErrorResponse(response, 401, "ACCESS_TOKEN_EXPIRED");
                return;
            }

            // Refresh Token 타입 오류
            if (e.getMessage().contains("Not a refresh token")) {
                setErrorResponse(response, 400, "INVALID_REFRESH_TOKEN");
                return;
            }

            // 토큰 타입 오류
            if (e.getMessage().contains("Invalid token type")) {
                setErrorResponse(response, 400, "INVALID_TOKEN_TYPE");
                return;
            }

            // 기타 JWT 관련 오류
            setErrorResponse(response,401, "INVALID_JWT");

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
