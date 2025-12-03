package com.example.umc9th.global.auth.scheduler;

import com.example.umc9th.global.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token 자동 정리 스케줄러
 * - 매일 새벽 3시에 실행
 * - 만료된 토큰을 DB에서 삭제하여 용량 절약
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 Refresh Token 자동 삭제
     * - 매일 새벽 3시 실행
     * - Cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("[TokenCleanupScheduler.cleanupExpiredTokens] 만료된 토큰 정리 시작");

        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteAllExpiredTokens(now);

        log.info("[TokenCleanupScheduler.cleanupExpiredTokens] 만료된 토큰 정리 완료");
    }

    /**
     * 테스트용: 1시간마다 실행 (선택 사항)
     * - 개발 환경에서 빠른 테스트를 위해 사용
     * - Production에서는 주석 처리 권장
     */
    // @Scheduled(cron = "0 0 * * * *")  // 매시간 정각
    // @Transactional
    // public void cleanupExpiredTokensHourly() {
    //     log.info("[TokenCleanupScheduler.cleanupExpiredTokensHourly] 만료된 토큰 정리 (시간별)");
    //     refreshTokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    // }
}
