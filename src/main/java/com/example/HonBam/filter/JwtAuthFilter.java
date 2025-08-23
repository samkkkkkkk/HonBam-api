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

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 공개/예외 경로는 필터 스킵 (SecurityConfig와 일치하도록 유지)
    private static final String[] SKIP_PATTERNS = {
            "/",
            "/error",
            "/api/recipe/**",
            "/api/freeboard/**",
            "/api/posts/**",
            "/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**"
    };
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // OPTIONS 프리플라이트는 항상 스킵
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

        // 이미 인증돼 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1) 쿠키 access_token → 2) Authorization: Bearer
        String token = extractCookie(request, "access_token");
        if (!StringUtils.hasText(token)) {
            token = extractBearer(request);
        }

        // 토큰 없으면 패스(컨트롤러에서 인증 필요 시 401)
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
            // 유효하지 않은 토큰 → 인증 세팅 없이 진행(EntryPoint/AccessDeniedHandler에서 처리)
            log.warn("JWT 검증 실패: {}", e.getMessage());
        } catch (Exception e) {
            // 기타 예외는 로깅 후 계속 진행(전역 예외필터에서 잡을 수 있음)
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
}
