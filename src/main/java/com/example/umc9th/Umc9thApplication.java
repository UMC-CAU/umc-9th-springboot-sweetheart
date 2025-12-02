package com.example.umc9th;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // 스케줄링 활성화 (만료된 토큰 자동 정리)
// ⭐ @EnableJpaAuditing은 global.config.JpaConfig로 분리 (테스트 호환성을 위해)
public class Umc9thApplication {

    public static void main(String[] args) {
        SpringApplication.run(Umc9thApplication.class, args);
    }

}
