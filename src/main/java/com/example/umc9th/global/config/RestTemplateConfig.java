package com.example.umc9th.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 *
 * 외부 API 호출(디스코드 웹훅 등)을 위한 RestTemplate 빈을 생성합니다.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate 빈 생성
     *
     * - 연결 타임아웃: 5초 (5000ms)
     * - 읽기 타임아웃: 5초 (5000ms)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
