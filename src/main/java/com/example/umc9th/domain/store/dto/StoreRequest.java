package com.example.umc9th.domain.store.dto;

import com.example.umc9th.global.validation.annotation.ExistFood;
import com.example.umc9th.global.validation.annotation.ExistLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 가게 관련 요청 DTO 모음
 */
public class StoreRequest {

    /**
     * 가게 추가 요청 DTO
     */
    @Schema(description = "가게 추가 요청")
    public record CreateStoreDTO(
            @Schema(description = "가게 이름", example = "반이학생")
            @NotBlank(message = "가게 이름은 필수입니다")
            @Size(min = 1, max = 100, message = "가게 이름은 1자 이상 100자 이하여야 합니다")
            String name,

            @Schema(description = "매니저 전화번호", example = "1012345678")
            @NotNull(message = "매니저 전화번호는 필수입니다")
            Long managerNumber,

            @Schema(description = "상세 주소", example = "서울특별시 강남구 테헤란로 427")
            @NotBlank(message = "상세 주소는 필수입니다")
            @Size(min = 5, max = 200, message = "상세 주소는 5자 이상 200자 이하여야 합니다")
            String detailAddress,

            @Schema(description = "지역 ID", example = "1")
            @NotNull(message = "지역 ID는 필수입니다")
            @ExistLocation
            Long locationId,

            @Schema(description = "음식 카테고리 ID", example = "1")
            @NotNull(message = "음식 카테고리 ID는 필수입니다")
            @ExistFood
            Long foodId
    ) {}
}
