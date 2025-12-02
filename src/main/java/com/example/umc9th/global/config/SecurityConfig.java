package com.example.umc9th.global.config;

import com.example.umc9th.global.auth.CustomUserDetailsService;
import com.example.umc9th.global.auth.jwt.JwtAuthFilter;
import com.example.umc9th.global.auth.jwt.JwtUtil;
import com.example.umc9th.global.auth.oauth2.CustomOAuth2UserService;
import com.example.umc9th.global.auth.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정 클래스 (JWT + OAuth2 방식)
 * - Stateless 인증: 서버에 세션을 저장하지 않음
 * - JWT 토큰 기반 인증/인가
 * - Google OAuth2 로그인 지원
 * - CORS 설정 적용
 */
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * 인증 없이 접근 가능한 URI 목록
     */
    private final String[] allowUris = {
            "/api/members/sign-up",  // 회원가입
            "/api/members/login",    // 로그인
            "/",  // 루트 (Swagger UI)
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/login/oauth2/**",  // OAuth2 로그인
            "/oauth2/**",        // OAuth2 콜백
    };

    /**
     * Spring Security Filter Chain 설정
     * - JWT 토큰 기반 인증
     * - Stateless 세션 정책
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // HTTP 요청에 대한 접근 제어 설정
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowUris).permitAll()  // 허용 URI는 인증 불필요
                        .requestMatchers("/admin/**").hasRole("ADMIN")  // 관리자만 접근 가능
                        .anyRequest().authenticated()  // 그 외 모든 요청은 인증 필요
                )
                // 폼 로그인 비활성화 (JWT 사용)
                .formLogin(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // CSRF 보호 비활성화 (Stateless이므로 불필요)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 정책: STATELESS (세션 사용 안 함)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)  // OAuth2 사용자 정보 처리
                        )
                        .successHandler(oAuth2SuccessHandler)  // 로그인 성공 핸들러 (JWT 발급)
                )
                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 위치)
                // Bean으로 등록하지 않고 직접 인스턴스 생성 (Bean 중복 방지)
                .addFilterBefore(new JwtAuthFilter(jwtUtil, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/members/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     * BCrypt 해시 알고리즘을 사용하여 비밀번호를 단방향 암호화
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
