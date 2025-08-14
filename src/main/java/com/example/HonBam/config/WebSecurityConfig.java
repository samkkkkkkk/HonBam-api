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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

// @Configuration // 설정 클래스 용도로 사용하도록 스프링에 등록하는 아노테이션
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
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
                // CSRF
                // 크로스 도메인 + SameSite=None 사용 시에는 CSRF 토큰 전략을 권장한다.
                // 우선 개발 단계에서는 disable로 두고, 프론트에서 XSRF 헤더 붙일 준비가 되면 아래 주석을 해제:
                // .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                // 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // === 인증 필요한 엔드포인트(우선 선언해서 우선순위 보장) ===
                        .requestMatchers(HttpMethod.POST, "/api/auth/paypromote").authenticated()
                        .requestMatchers("/api/auth/profile-image").authenticated()
                        .requestMatchers("/api/tosspay/info").authenticated()
                        .requestMatchers("/api/tosspay/confirm").authenticated()

                        // === 공개 엔드포인트 ===
                        .requestMatchers("/", "/api/auth/**").permitAll()
                        .requestMatchers("/api/recipe").permitAll()
                        .requestMatchers("/api/freeboard").permitAll()
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/ws-chat/**", "/chat/**", "/redis/**", "/chatRooms/**", "/topic/**", "/app/**").permitAll()

                        // 주의: 과거에 `/api/tosspay/**` 전체를 permitAll로 뚫어놨는데,
                        // 위에 일부를 authenticated로 보호하려면 와일드카드 permitAll은 제거해야 한다.
                        // .requestMatchers("/api/tosspay/**").permitAll()  // ← 제거

                        // 나머지는 인증
                        .anyRequest().authenticated()
                );

        // 필터 순서: Exception → (CORS 통과) → JWT
        http.addFilterBefore(jwtExceptionFilter, JwtAuthFilter.class);
        http.addFilterAfter(jwtAuthFilter, CorsFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var conf = new CorsConfiguration();

        // allowCredentials=true이면 * 금지. 프론트 도메인을 정확히 명시.
        conf.setAllowCredentials(true);
        conf.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://your-frontend.example.com"
        ));
        conf.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        conf.setAllowedHeaders(Arrays.asList("Content-Type","Authorization","X-CSRF-Token"));
        // 필요 시 노출 헤더: conf.setExposedHeaders(List.of("Set-Cookie"));
        conf.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);
        return source;
    }


}






