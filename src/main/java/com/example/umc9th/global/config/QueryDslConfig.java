package com.example.umc9th.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정 클래스
 * JPAQueryFactory를 Spring Bean으로 등록하여 전역에서 사용 가능하도록 설정
 */
@Configuration
@RequiredArgsConstructor
public class QueryDslConfig {

    private final EntityManager entityManager;

    /**
     * JPAQueryFactory 빈 등록
     * @return JPAQueryFactory 인스턴스 (QueryDSL 쿼리 작성용)
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
