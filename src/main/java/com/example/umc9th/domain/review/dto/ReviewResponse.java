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
     * 리뷰 목록 조회 응답 DTO (페이징 포함)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReviewPreViewListDTO {
        private java.util.List<MyReview> reviewList;
        private Integer listSize;
        private Integer totalPage;
        private Long totalElements;
        private Boolean isFirst;
        private Boolean isLast;
    }
}
