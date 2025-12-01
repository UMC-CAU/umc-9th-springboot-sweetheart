package com.example.umc9th.domain.review.dto;

import com.example.umc9th.domain.review.entity.Review;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Review 도메인 응답 DTO 모음
 */
public class ReviewResponse {

    /**
     * 리뷰 작성 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "리뷰 작성 응답")
    public static class CreateReview {
        @Schema(description = "생성된 리뷰 ID", example = "1")
        private Long reviewId;

        @Schema(description = "리뷰 작성 시간", example = "2025-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static CreateReview from(Review review) {
            return CreateReview.builder()
                    .reviewId(review.getId())
                    .createdAt(review.getCreatedAt())
                    .build();
        }
    }

    /**
     * 내 리뷰 조회 응답 DTO
     * 회원이 작성한 리뷰 목록을 조회할 때 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MyReview {
        private Long id;
        private String content;
        private Float star;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        private StoreInfo store;

        public static MyReview from(Review review) {
            return MyReview.builder()
                    .id(review.getId())
                    .content(review.getContent())
                    .star(review.getStar())
                    .createdAt(review.getCreatedAt())
                    .store(StoreInfo.from(review.getStore()))
                    .build();
        }
    }

    /**
     * 리뷰에 포함된 가게 정보 DTO
     * MyReview 내부에서 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String address;

        public static StoreInfo from(com.example.umc9th.domain.store.entity.Store store) {
            return StoreInfo.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .address(store.getDetailAddress())
                    .build();
        }
    }

    /**
     * 리뷰 목록 조회 응답 DTO (페이징 포함 - Page 사용)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "리뷰 목록 조회 응답 (Page)")
    public static class ReviewPreViewListDTO {
        @Schema(description = "리뷰 목록")
        private java.util.List<MyReview> reviewList;

        @Schema(description = "현재 페이지 리뷰 개수", example = "10")
        private Integer listSize;

        @Schema(description = "전체 페이지 수", example = "5")
        private Integer totalPage;

        @Schema(description = "전체 리뷰 개수", example = "50")
        private Long totalElements;

        @Schema(description = "첫 페이지 여부", example = "true")
        private Boolean isFirst;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private Boolean isLast;
    }

    /**
     * 리뷰 목록 조회 응답 DTO (무한 스크롤 - Slice 사용)
     *
     * Page와의 차이점:
     * - totalPage, totalElements가 없음 (COUNT 쿼리 실행하지 않음)
     * - hasNext로 다음 페이지 존재 여부만 확인
     * - 무한 스크롤 UI에 최적화
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "리뷰 목록 조회 응답 (Slice - 무한 스크롤)")
    public static class ReviewPreViewSliceDTO {
        @Schema(description = "리뷰 목록")
        private java.util.List<MyReview> reviewList;

        @Schema(description = "현재 페이지 리뷰 개수", example = "10")
        private Integer listSize;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;

        @Schema(description = "첫 페이지 여부", example = "true")
        private Boolean isFirst;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private Boolean isLast;
    }
}
