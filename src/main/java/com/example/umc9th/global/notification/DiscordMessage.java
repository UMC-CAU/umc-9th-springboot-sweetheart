package com.example.umc9th.global.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 디스코드 웹훅 메시지 구조를 정의하는 DTO
 * Discord Webhook API 명세에 따라 embeds 배열 형식으로 메시지를 전송합니다
 */
@Getter
@Builder
public class DiscordMessage {
    private List<Embed> embeds;

    /**
     * 디스코드 임베드 메시지 구조
     */
    @Getter
    @Builder
    public static class Embed {
        private String title;           // 임베드 제목
        private String description;     // 임베드 설명
        private Integer color;          // 임베드 왼쪽 색상 바 (10진수)
        private List<Field> fields;     // 필드 목록
        private String timestamp;       // ISO 8601 포맷 타임스탬프

        /**
         * 임베드 내부 필드 구조
         */
        @Getter
        @Builder
        public static class Field {
            private String name;        // 필드 이름
            private String value;       // 필드 값
            private Boolean inline;     // 인라인 여부 (가로로 나란히 배치)
        }
    }

    /**
     * 500 에러 알림을 위한 임베드 메시지 생성
     */
    public static DiscordMessage createErrorMessage(
            String path,
            String errorMessage,
            String exceptionType,
            String traceId,
            String timestamp) {

        Embed.Field pathField = Embed.Field.builder()
                .name("📍 요청 경로")
                .value(String.format("`%s`", path))
                .inline(true)
                .build();

        Embed.Field timeField = Embed.Field.builder()
                .name("⏰ 발생 시각")
                .value(timestamp)
                .inline(true)
                .build();

        Embed.Field typeField = Embed.Field.builder()
                .name("🔍 예외 타입")
                .value(String.format("`%s`", exceptionType))
                .inline(false)
                .build();

        Embed.Field messageField = Embed.Field.builder()
                .name("💬 에러 메시지")
                .value(errorMessage != null && !errorMessage.isEmpty()
                        ? String.format("```%s```", errorMessage)
                        : "_(메시지 없음)_")
                .inline(false)
                .build();

        Embed.Field.FieldBuilder traceFieldBuilder = Embed.Field.builder()
                .name("🔗 Trace ID")
                .inline(true);

        if (traceId != null && !traceId.isEmpty()) {
            traceFieldBuilder.value(String.format("`%s`", traceId));
        } else {
            traceFieldBuilder.value("_(없음)_");
        }

        Embed.Field traceField = traceFieldBuilder.build();

        Embed embed = Embed.builder()
                .title("🚨 500 Internal Server Error 발생!")
                .description("서버에서 예상치 못한 오류가 발생했습니다")
                .color(15158332)  // 빨간색 (#E74C3C)
                .fields(List.of(pathField, timeField, typeField, messageField, traceField))
                .timestamp(java.time.Instant.now().toString())
                .build();

        return DiscordMessage.builder()
                .embeds(List.of(embed))
                .build();
    }
}
