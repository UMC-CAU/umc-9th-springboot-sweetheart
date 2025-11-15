package com.example.umc9th.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 *
 * @Async 어노테이션을 활성화합니다.
 * 디스코드 웹훅 전송 등 시간이 걸리는 작업을 비동기로 처리하여
 * API 응답 시간에 영향을 주지 않도록 합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // @EnableAsync만으로도 기본 설정이 충분합니다
    // 필요시 ThreadPoolTaskExecutor를 커스터마이징할 수 있습니다
}
