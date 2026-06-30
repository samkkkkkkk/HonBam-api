package com.example.HonBam.config;

import com.example.HonBam.auth.service.CustomOAuth2UserService;
import com.example.HonBam.auth.OAuth2FailureHandler;
import com.example.HonBam.auth.OAuth2SuccessHandler;
import com.example.HonBam.filter.JwtAuthFilter;
import com.example.HonBam.filter.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@Profile("!test")
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable()) // ← 기본 로그인폼 redirect 방지
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 인증 실패 시 401 반환
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"UNAUTHORIZED\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 인가 실패 시 403 반환
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"FORBIDDEN\"}");
                        })
                )

                .authorizeRequests(auth -> auth
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개: 회원가입/로그인/중복검사/refresh/소셜 로그인
                        .antMatchers(HttpMethod.POST, "/api/users").permitAll() // 회원가입
                        .antMatchers("/api/auth/login").permitAll()
                        .antMatchers("/api/users/check").permitAll()
                        .antMatchers("/api/auth/refresh").permitAll()
                        .antMatchers("/oauth2/**").permitAll()

                        // 공개 전용 GET (인증 불필요)
                        .antMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/recipe/**").permitAll()

                        // swagger
                        .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-resources", "/webjars/**").permitAll()

                        // upload: 프로필 presigned 발급만 공개, 그 외 인증
                        // (더 구체적인 permitAll 규칙을 broad authenticated 규칙보다 먼저 선언)
                        .antMatchers(HttpMethod.POST, "/api/upload/presigned/profile").permitAll()
                        .antMatchers("/api/upload/**").authenticated()

                        // posts: GET 조회만 공개, 그 외 인증
                        .antMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .antMatchers("/api/posts/**").authenticated()

                        // freeboard: GET 조회만 공개, 그 외 인증
                        .antMatchers(HttpMethod.GET, "/api/freeboard/**").permitAll()
                        .antMatchers("/api/freeboard/**").authenticated()

                        // sns: feed GET만 공개, 그 외 전체 보호
                        .antMatchers(HttpMethod.GET, "/api/sns/feed/**").permitAll()
                        .antMatchers("/api/sns/**").authenticated()

                        // 채팅 API
                        .antMatchers("/api/chat/**").authenticated()

                        // 웹소켓 handshake 도메인 인증
                        .antMatchers("/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**").permitAll()

                        // auth 추가 기능
                        .antMatchers("/api/users/paypromote").authenticated()
                        .antMatchers("/api/users/profile-image").authenticated()
                        .antMatchers("/api/users/profile-s3").authenticated()
                        .antMatchers("/api/users/userinfo").authenticated()
                        .antMatchers("/api/users/verify").authenticated()
                        .antMatchers("/api/auth/logout").authenticated()
                        .antMatchers("/api/users/delete").authenticated()

                        // toss 결제 시스템
                        .antMatchers("/api/tosspay/info").authenticated()
                        .antMatchers("/api/tosspay/confirm").authenticated()

                        // 루트 및 에러
                        .antMatchers("/", "/error").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );


        // ===== 필터 순서: 예외 → JWT → UsernamePasswordAuthenticationFilter 이전 =====
        http.addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration conf = new CorsConfiguration();
        conf.setAllowCredentials(true);
        conf.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        conf.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        conf.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-CSRF-Token"));
        // 필요 시 클라이언트에 노출할 헤더
        // conf.setExposedHeaders(List.of("Set-Cookie"));
        conf.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);
        return source;
    }
}
