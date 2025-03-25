package com.example.HonBam.config;

import com.example.HonBam.filter.JwtAuthFilter;
import com.example.HonBam.filter.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
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
@EnableWebSecurity // 시큐리티 설정 파일로 사용할 클래스 선언.
@RequiredArgsConstructor
// 자동 권한 검사를 수행하기 위한 설정
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 시큐리티 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Security 모듈이 기본적으로 제공하는 보안 정책 해제.
        http
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()
                .httpBasic().disable()
                // 세션인증을 사용하지 않겠다.
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 어떤 요청에서 인증을 안 할 것인지, 언제 인증을 할 것인지 설정
                .authorizeRequests()
                // /api/auth/** 은 permit이지만, /promote는 검증이 필요하기 때문에 추가.(순서 조심!)
                .antMatchers(HttpMethod.POST, "/api/auth/paypromote")
                .authenticated()
                .antMatchers("/api/auth/load-profile").authenticated()
                .antMatchers("/api/recipe").permitAll()
                .antMatchers("/api/tosspay/info").authenticated()
                .antMatchers("/api/tosspay/confirm").authenticated()
                .antMatchers("/api/tosspay/**").permitAll()
                // '/api/auth'로 시작하는 요청과 '/'요청은 권한 검사 없이 허용하겠다.
                .antMatchers("/", "/api/auth/**").permitAll()
                .antMatchers("/api/freeboard").permitAll()
                .antMatchers("/api/posts/**").permitAll()
                .antMatchers("/ws-chat/**").permitAll()
                .antMatchers("redis/**").permitAll()
                // '/api/HonBams'라는 요청이 POST로 들어오고, Role 값이 ADMIN인 경우 권한 검사 없이 허용하겠다.
//                .antMatchers(HttpMethod.POST, "/api/HonBams").hasRole("ADMIN").permitAll()
                // 위에서 따로 설정하지 않은 나머지 요청들은 권한 검사가 필요하다.
                .anyRequest().authenticated();

        // 토큰 인증 필터 연결
        // jwtAuthFilter부터 연결 -> CORS 필터를 이후에 통과하도록 설정.
        http.addFilterAfter(
                jwtAuthFilter,
                CorsFilter.class // import 주의: 스프링 꺼로
        );

        // Exception Filter를 Auth Filter 앞에 배치를 하겠다는 뜻.
        // Filter 역할을 하는 클래스는 Spring Container 내부에 배치되는 것이 아니기 때문에
        // Spring이 제공하는 예외 처리 등이 힘들 수 있다.
        // 예외 처리만을 전담하는 필터를 생성해서, 예외가 발생하는 필터 앞단에 배치하면 예외가 먼저 배치된 필터로
        // 넘어가서 처리가 가능하게 됩니다.

        http.addFilterBefore(jwtExceptionFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("HEAD","POST","GET","DELETE","PUT"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



}






