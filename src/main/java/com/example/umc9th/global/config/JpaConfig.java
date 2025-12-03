package com.example.umc9th.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * ⭐ 왜 별도의 Configuration으로 분리했는가?
 * - Main Application 클래스에 @EnableJpaAuditing을 두면 테스트 시 문제 발생
 * - @WebMvcTest는 웹 계층만 테스트하는데, @EnableJpaAuditing은 JPA Auditing을 활성화함
 * - JPA Auditing은 jpaMappingContext Bean이 필요하지만, @WebMvcTest는 엔티티 스캔을 하지 않음
 * - 결과: "JPA metamodel must not be empty" 에러 발생
 *
 * 해결 방법:
 * - @EnableJpaAuditing을 별도 Configuration으로 분리
 * - 테스트에서는 이 Configuration이 로드되지 않음 (자동 제외)
 * - Main Application 클래스는 깔끔하게 유지
 */
@Configuration
@EnableJpaAuditing  // JPA Auditing 활성화 (createdAt, updatedAt 자동 설정)
public class JpaConfig {
}
