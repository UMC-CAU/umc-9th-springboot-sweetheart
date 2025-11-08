package com.example.umc9th.domain.store.controller;

import com.example.umc9th.domain.review.dto.ReviewDTO;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Store", description = "가게 관리 API")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final ReviewQueryService reviewQueryService;

    @Operation(
        summary = "특정 가게의 리뷰 조회",
        description = """
            가게에 작성된 리뷰를 조회합니다. (RESTful 자원 중심 설계)
            - 회원별 필터: memberId 파라미터
            - 별점 필터: minScore, maxScore 파라미터
            - 조건 조합 가능
            """
    )
    @GetMapping("/{storeId}/reviews")
    public ApiResponse<List<ReviewDTO.MyReview>> getStoreReviews(
            @Parameter(description = "가게 ID", required = true, example = "1")
            @PathVariable Long storeId,

            @Parameter(description = "회원 ID (특정 회원의 리뷰만 조회)", example = "1")
            @RequestParam(required = false) Long memberId,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam(required = false) Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam(required = false) Float maxScore
    ) {
        List<ReviewDTO.MyReview> reviews = reviewQueryService.getReviews(
                memberId, storeId, null, minScore, maxScore
        );
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }
}
