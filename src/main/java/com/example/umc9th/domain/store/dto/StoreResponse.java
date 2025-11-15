package com.example.umc9th.domain.store.dto;

import com.example.umc9th.domain.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Store 도메인 응답 DTO 모음
 */
public class StoreResponse {

    /**
     * 가게 추가 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가게 추가 응답")
    public static class CreateStore {
        @Schema(description = "생성된 가게 ID", example = "1")
        private Long storeId;

        @Schema(description = "가게 이름", example = "반이학생")
        private String name;

        @Schema(description = "지역명", example = "강남구")
        private String region;

        public static CreateStore from(Store store) {
            return CreateStore.builder()
                    .storeId(store.getId())
                    .name(store.getName())
                    .region(store.getLocation() != null ? store.getLocation().getName() : null)
                    .build();
        }
    }

    /**
     * 가게 검색 결과 응답 DTO
     * 지역 필터링, 검색 결과에 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SearchResult {
        private Long id;
        private String name;
        private String region;
        private String foodCategory;
        private String detailAddress;

        /**
         * Store 엔티티를 SearchResult DTO로 변환
         * Fetch Join으로 인해 location, food가 이미 로드된 상태여야 합니다.
         */
        public static SearchResult from(Store store) {
            return SearchResult.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .region(store.getLocation() != null ? store.getLocation().getName() : null)
                    .foodCategory(store.getFood() != null ? store.getFood().getName().name() : null)
                    .detailAddress(store.getDetailAddress())
                    .build();
        }
    }

    /**
     * 가게 간단 정보 응답 DTO
     * 다른 도메인에서 가게 정보를 참조할 때 사용
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleInfo {
        private Long id;
        private String name;
        private String region;

        public static SimpleInfo from(Store store) {
            return SimpleInfo.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .region(store.getLocation() != null ? store.getLocation().getName() : null)
                    .build();
        }
    }
}
