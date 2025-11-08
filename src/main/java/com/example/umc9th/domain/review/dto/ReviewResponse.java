package com.example.umc9th.domain.review.dto;

import com.example.umc9th.domain.review.entity.Review;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Review 도메인 응답 DTO 모음
 */
public class ReviewResponse {

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
}
