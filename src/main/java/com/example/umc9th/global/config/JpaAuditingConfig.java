package com.example.umc9th.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * @EnableJpaAuditing을 메인 애플리케이션에서 분리
 * - @WebMvcTest 사용 시 JPA Auditing 설정이 자동으로 제외됨
 * - BaseTimeEntity의 @CreatedDate, @LastModifiedDate가 정상 동작
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
