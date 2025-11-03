package com.example.HonBam.config;

import com.example.HonBam.auth.CustomOAuth2UserService;
import com.example.HonBam.auth.OAuth2FailureHandler;
import com.example.HonBam.auth.OAuth2SuccessHandler;
import com.example.HonBam.filter.JwtAuthFilter;
import com.example.HonBam.filter.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

                        // 공개 API
                        .antMatchers(HttpMethod.POST, "/api/auth").permitAll() // 회원가입
                        .antMatchers("/api/auth/login").permitAll()
                        .antMatchers("/api/auth/check").permitAll()
                        .antMatchers("/api/auth/refresh").permitAll()
                        .antMatchers("/oauth2/**").permitAll()
                        .antMatchers("/", "/error").permitAll()
                        .antMatchers("/api/recipe/**").permitAll()
                        .antMatchers("/api/freeboard/**").permitAll()
                        .antMatchers("/api/posts/**").permitAll()
                        .antMatchers("/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**").permitAll()
                        .antMatchers("/api/sns/feed/explore").permitAll()
                        .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-resources", "/webjars/**").permitAll()

                        // 인증 필요
                        .antMatchers("/api/chat/**").authenticated()
                        .antMatchers("/api/auth/paypromote").authenticated()
                        .antMatchers("/api/auth/profile-image").authenticated()
                        .antMatchers("/api/auth/profile-s3").authenticated()
                        .antMatchers("/api/auth/userinfo").authenticated()
                        .antMatchers("/api/auth/verify").authenticated()
                        .antMatchers("/api/auth/logout").authenticated()
                        .antMatchers("/api/auth/delete").authenticated()
                        .antMatchers("/api/tosspay/info").authenticated()
                        .antMatchers("/api/tosspay/confirm").authenticated()
                        .antMatchers("/api/sns/**").authenticated()

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
