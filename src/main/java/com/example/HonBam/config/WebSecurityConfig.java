package com.example.HonBam.config;

import com.example.HonBam.filter.JwtAuthFilter;
import com.example.HonBam.filter.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true) // 5.x 스타일
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF (개발 단계 비활성화; 운영 전 전환 고려)
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                // 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ===== 인가 규칙: 5.x는 authorizeRequests + antMatchers =====
                .authorizeRequests(auth -> auth
                        // Preflight 전부 허용
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 필요한 엔드포인트(먼저 선언해서 우선순위 확보)
                        .antMatchers( "/api/auth/paypromote").authenticated()
                        .antMatchers("/api/auth/profile-image").authenticated()
                        .antMatchers("/api/auth/userinfo").authenticated()
                        .antMatchers("/api/tosspay/info").authenticated()
                        .antMatchers("/api/tosspay/confirm").authenticated()
                        .antMatchers("/api/auth/verify").authenticated()

                        // 공개 엔드포인트
                        .antMatchers("/", "/error").permitAll()
                        .antMatchers("/api/auth/**").permitAll()
                        .antMatchers("/api/recipe/**").permitAll()
                        .antMatchers("/api/freeboard/**").permitAll()
                        .antMatchers("/api/posts/**").permitAll()
                        .antMatchers("/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**").permitAll()

                        // 나머지는 인증
                        .anyRequest().authenticated()
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
        // 개발 편의: 다양한 로컬 포트 대응
        conf.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://your-frontend.example.com"
        ));
        conf.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        conf.setAllowedHeaders(Arrays.asList("Content-Type","Authorization","X-CSRF-Token"));
        // 필요 시 노출 헤더
        // conf.setExposedHeaders(List.of("Set-Cookie"));
        conf.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);
        return source;
    }
}
