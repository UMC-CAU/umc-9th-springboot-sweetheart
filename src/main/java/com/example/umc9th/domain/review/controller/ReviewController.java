package com.example.umc9th.domain.review.controller;

import com.example.umc9th.domain.review.dto.ReviewRequest;
import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.service.ReviewCommandService;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "리뷰 관리", description = "리뷰 조회/작성 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewQueryService reviewQueryService;
    private final ReviewCommandService reviewCommandService;

    @Operation(
            summary = "가게에 리뷰 추가하기",
            description = """
                가게에 새로운 리뷰를 작성합니다.

                **Validation:**
                - storeId: 가게 ID 필수, DB에 존재해야 함
                - memberId: 회원 ID 필수, DB에 존재해야 함 (현재는 하드코딩, 추후 인증에서 자동 추출)
                - content: 리뷰 내용 필수, 10자 이상 500자 이하
                - star: 별점 필수, 0.0 ~ 5.0 범위
                """
    )
    @PostMapping
    public ApiResponse<ReviewResponse.CreateReview> createReview(
            @Valid @RequestBody ReviewRequest.CreateReviewDTO request
    ) {
        ReviewResponse.CreateReview response = reviewCommandService.createReview(request);
        return ApiResponse.onSuccess(SuccessCode.CREATED, response);
    }

    @Operation(
        summary = "리뷰 통합 조회 (RESTful)",
        description = """
            모든 필터 조건을 지원하는 통합 리뷰 조회 API

            **사용 예시:**
            - 전체 리뷰: 파라미터 없음
            - 특정 회원 리뷰: memberId만 지정
            - 특정 가게 리뷰: storeId만 지정
            - 가게 이름 검색: storeName 지정
            - 별점 필터: minScore, maxScore 지정
            - 조건 조합: 여러 파라미터 자유롭게 조합

            **RESTful 권장 사항:**
            - 회원 중심 조회: GET /api/members/{memberId}/reviews 사용 권장
            - 가게 중심 조회: GET /api/stores/{storeId}/reviews 사용 권장
            """
    )
    @GetMapping
    public ApiResponse<List<ReviewResponse.MyReview>> getReviews(
            @Parameter(description = "회원 ID", example = "1")
            @RequestParam(required = false) Long memberId,

            @Parameter(description = "가게 ID", example = "5")
            @RequestParam(required = false) Long storeId,

            @Parameter(description = "가게 이름 (부분 일치)", example = "반이학생")
            @RequestParam(required = false) String storeName,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam(required = false) Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam(required = false) Float maxScore
    ) {
        List<ReviewResponse.MyReview> reviews = reviewQueryService.getReviews(
                memberId, storeId, storeName, minScore, maxScore
        );
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }
}
