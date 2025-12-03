package com.example.umc9th.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS (Cross-Origin Resource Sharing) 설정
 * - 다른 도메인에서 API 호출을 허용
 * - 프론트엔드와 백엔드가 다른 도메인일 때 필요
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (도메인) 설정
        // ⚠️ Production에서는 실제 프론트엔드 도메인으로 변경하세요!
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",              // Local React/Next.js
                "http://localhost:5173",              // Local Vite
                "https://spring-swagger-api.log8.kr",  // Production 백엔드 (Swagger 테스트용)
                "https://your-frontend-domain.com"    // Production 프론트엔드 (실제 도메인으로 변경)
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",  // JWT 토큰
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (초 단위)
        configuration.setMaxAge(3600L);

        // 노출할 헤더 (클라이언트가 접근 가능한 응답 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        // URL 패턴별 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 적용

        return source;
    }
}
