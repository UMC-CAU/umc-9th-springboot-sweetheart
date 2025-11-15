package com.example.umc9th.global.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 디스코드 웹훅을 통해 에러 알림을 전송하는 서비스
 *
 * 비동기(@Async)로 동작하여 API 응답 시간에 영향을 주지 않습니다.
 */
@Slf4j
@Service
public class DiscordWebhookService {

    private final RestTemplate restTemplate;
    private final String webhookUrl;
    private final boolean enabled;

    public DiscordWebhookService(
            RestTemplate restTemplate,
            @Value("${discord.webhook.url:}") String webhookUrl,
            @Value("${discord.webhook.enabled:false}") boolean enabled) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
    }

    /**
     * 500 에러 정보를 디스코드로 전송
     *
     * @param path 요청 경로
     * @param errorMessage 에러 메시지
     * @param exceptionType 예외 타입
     * @param traceId 추적 ID
     * @param timestamp 발생 시각
     */
    @Async
    public void sendErrorNotification(
            String path,
            String errorMessage,
            String exceptionType,
            String traceId,
            String timestamp) {

        // 웹훅이 비활성화되어 있거나 URL이 설정되지 않은 경우 전송하지 않음
        if (!enabled) {
            log.debug("[DiscordWebhookService] 웹훅이 비활성화되어 있습니다 (enabled=false)");
            return;
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("[DiscordWebhookService] 웹훅 URL이 설정되지 않았습니다");
            return;
        }

        try {
            log.info("[DiscordWebhookService.sendErrorNotification] 디스코드 알림 전송 시작 - path: {}", path);

            // 디스코드 메시지 생성
            DiscordMessage message = DiscordMessage.createErrorMessage(
                    path, errorMessage, exceptionType, traceId, timestamp
            );

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // HTTP 요청 엔티티 생성
            HttpEntity<DiscordMessage> request = new HttpEntity<>(message, headers);

            // 디스코드 웹훅으로 POST 요청 전송
            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("[DiscordWebhookService] 디스코드 알림 전송 성공 - path: {}", path);

        } catch (Exception e) {
            // 웹훅 전송 실패 시 로그만 남기고 예외를 던지지 않음 (원래 에러 처리에 영향 X)
            log.error("[DiscordWebhookService] 디스코드 알림 전송 실패 - path: {}, error: {}",
                    path, e.getMessage(), e);
        }
    }
}
