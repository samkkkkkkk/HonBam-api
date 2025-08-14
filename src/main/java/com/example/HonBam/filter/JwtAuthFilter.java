package com.example.HonBam.filter;

import com.example.HonBam.auth.CustomUserDetailsService;
import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 클라이언트가 전송한 토큰을 검사하는 필터
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 공개/예외 경로는 필터 스킵 (필요에 맞게 추가/수정)
    private static final String[] SKIP_PATTERNS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/check",
            "/api/auth",              // 회원가입 multipart
            "/api/recipe/**",
            "/api/posts/**",
            "/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**",
            "/", "/error"
    };
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // OPTIONS 프리플라이트는 스킵
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        for (String pattern : SKIP_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 이미 인증돼 있으면 스킵
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1) 쿠키에서 access_token → 2) Authorization: Bearer 보조
        String token = extractCookie(request, "access_token");
        if (token == null) {
            token = extractBearer(request);
        }

        // 토큰 없으면 패스
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // access 전용 검증(+서명/만료)
            TokenUserInfo info = tokenProvider.validateAndGetTokenUserInfo(token);

            var userDetails = customUserDetailsService.loadUserByUsername(info.getUserId());
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            // 유효하지 않은 토큰인 경우 → 인증 없이 계속 진행(컨트롤러/EntryPoint에서 401 처리)
            log.warn("JWT 검증 실패: {}", e.getMessage());
        } catch (Exception e) {
            // 기타 예외는 예외 필터에서 처리되도록 던지지 않고 로그만 남김
            log.warn("JWT 필터 처리 중 예외: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String extractBearer(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private String parseBearerToken(HttpServletRequest request) {

        // 요청 헤더에서 토큰 꺼내오기
        // -- content-type : application/json
        // -- Authorization : Bearer asboeo3n4ok4nosnkdl...
        String bearerToken = request.getHeader("Authorization");

        // 요청 헤더에서 가져온 토큰은 순수 토큰 값이 아닌
        // 앞에 Bearer가 붙어있으니 이것을 제거하는 작업
        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}










