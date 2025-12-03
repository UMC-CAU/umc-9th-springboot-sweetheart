package com.example.umc9th.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트 환경용 Security 설정
 * - 모든 요청을 인증 없이 허용
 * - Security 필터 체인을 우회
 * - @TestConfiguration으로 테스트 환경에서만 활성화
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * 테스트용 Security Filter Chain
     * - 모든 요청 허용
     * - CSRF 비활성화
     * - Form Login 비활성화
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 모든 요청 허용
                );

        return http.build();
    }
}
