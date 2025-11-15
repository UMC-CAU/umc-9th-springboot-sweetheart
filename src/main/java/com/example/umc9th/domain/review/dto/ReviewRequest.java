package com.example.umc9th.domain.review.dto;

import com.example.umc9th.global.validation.annotation.ExistMember;
import com.example.umc9th.global.validation.annotation.ExistStore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 리뷰 관련 요청 DTO 모음
 */
public class ReviewRequest {

    /**
     * 리뷰 작성 요청 DTO
     */
    @Schema(description = "리뷰 작성 요청")
    public record CreateReviewDTO(
            @Schema(description = "가게 ID", example = "1")
            @NotNull(message = "가게 ID는 필수입니다")
            @ExistStore
            Long storeId,

            @Schema(description = "회원 ID (현재는 하드코딩, 추후 인증에서 자동 추출)", example = "1")
            @NotNull(message = "회원 ID는 필수입니다")
            @ExistMember
            Long memberId,

            @Schema(description = "리뷰 내용", example = "음식이 정말 맛있어요!")
            @NotBlank(message = "리뷰 내용은 필수입니다")
            @Size(min = 10, max = 500, message = "리뷰 내용은 10자 이상 500자 이하여야 합니다")
            String content,

            @Schema(description = "별점 (0.0 ~ 5.0)", example = "4.5")
            @NotNull(message = "별점은 필수입니다")
            @DecimalMin(value = "0.0", message = "별점은 0.0 이상이어야 합니다")
            @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
            Float star
    ) {}
}
