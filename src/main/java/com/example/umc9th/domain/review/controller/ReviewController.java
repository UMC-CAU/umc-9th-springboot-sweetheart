package com.example.umc9th.domain.review.controller;

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

@Tag(name = "리뷰 관리", description = "리뷰 조회 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewQueryService reviewQueryService;

    @Operation(
        summary = "내가 작성한 리뷰 조회",
        description = """
            동적 필터링을 지원하는 리뷰 조회 API
            - 가게별: storeName 파라미터
            - 별점 이상: minScore만 지정
            - 별점 범위: minScore + maxScore 지정
            - 조합: 위 조건들을 자유롭게 조합 가능
            """
    )
    @GetMapping("/my")
    public ApiResponse<List<ReviewDTO.MyReview>> getMyReviews(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @RequestParam Long memberId,

            @Parameter(description = "가게 이름 (부분 일치)", example = "반이학생")
            @RequestParam(required = false) String storeName,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam(required = false) Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam(required = false) Float maxScore
    ) {
        List<ReviewDTO.MyReview> reviews = reviewQueryService.getMyReviews(memberId, storeName, minScore, maxScore);
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }

    @Operation(summary = "별점 범위로 리뷰 조회", description = "별점 범위로 필터링")
    @GetMapping("/my/star-range")
    public ApiResponse<List<ReviewDTO.MyReview>> getMyReviewsByStarRange(
            @Parameter(description = "회원 ID", required = true)
            @RequestParam Long memberId,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam Float maxScore
    ) {
        List<ReviewDTO.MyReview> reviews = reviewQueryService.getMyReviewsByStarRange(memberId, minScore, maxScore);
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }

    @Operation(summary = "특정 가게의 내 리뷰 조회", description = "가게 ID로 리뷰 조회")
    @GetMapping("/my/store/{storeId}")
    public ApiResponse<List<ReviewDTO.MyReview>> getMyReviewsByStore(
            @Parameter(description = "회원 ID", required = true)
            @RequestParam Long memberId,

            @Parameter(description = "가게 ID", required = true)
            @PathVariable Long storeId
    ) {
        List<ReviewDTO.MyReview> reviews = reviewQueryService.getMyReviewsByStore(memberId, storeId);
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }
}
