package com.example.umc9th.domain.review.dto;

import com.example.umc9th.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class ReviewDTO {

    @Getter
    @AllArgsConstructor
    public static class MyReview {
        private Long id;
        private String content;
        private Float star;
        private LocalDateTime createdAt;
        private StoreInfo store;

        public static MyReview from(Review review) {
            return new MyReview(
                review.getId(),
                review.getContent(),
                review.getStar(),
                review.getCreatedAt(),
                StoreInfo.from(review.getStore())
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String address;

        public static StoreInfo from(com.example.umc9th.domain.store.entity.Store store) {
            return new StoreInfo(
                store.getId(),
                store.getName(),
                store.getDetailAddress()
            );
        }
    }
}
