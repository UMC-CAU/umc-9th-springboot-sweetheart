package com.example.umc9th.domain.store.controller;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.domain.store.dto.StoreRequest;
import com.example.umc9th.domain.store.dto.StoreResponse;
import com.example.umc9th.domain.store.service.StoreCommandService;
import com.example.umc9th.domain.store.service.StoreQueryService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "가게 관리", description = "가게 조회/추가 API")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final ReviewQueryService reviewQueryService;
    private final StoreQueryService storeQueryService;
    private final StoreCommandService storeCommandService;

    @Operation(
            summary = "특정 지역에 가게 추가하기",
            description = """
                특정 지역에 새로운 가게를 추가합니다.

                **Validation:**
                - name: 가게 이름 필수, 1자 이상 100자 이하
                - managerNumber: 매니저 전화번호 필수
                - detailAddress: 상세 주소 필수, 5자 이상 200자 이하
                - locationId: 지역 ID 필수, DB에 존재해야 함
                - foodId: 음식 카테고리 ID 필수, DB에 존재해야 함
                """
    )
    @PostMapping
    public ApiResponse<StoreResponse.CreateStore> createStore(
            @Valid @RequestBody StoreRequest.CreateStoreDTO request
    ) {
        StoreResponse.CreateStore response = storeCommandService.createStore(request);
        return ApiResponse.onSuccess(SuccessCode.CREATED, response);
    }

    @Operation(
        summary = "가게 검색 (지역 필터링 + 이름 검색 + 정렬)",
        description = """
            지역과 이름 기반으로 가게를 검색하고 정렬합니다.

            **지역 필터링 (다중 선택 가능):**
            - regions 파라미터를 여러 번 전달
            - 예시: ?regions=강남구&regions=서초구
            - regions를 생략하면 전체 지역 조회

            **이름 검색 (공백 처리):**
            - 공백 있음: 각 단어를 OR 조건으로 검색 (합집합)
              예: "민트 초코" → "민트"가 포함되거나 "초코"가 포함된 가게 모두 조회
            - 공백 없음: 전체 문자열로 검색
              예: "민트초코" → "민트초코"가 포함된 가게만 조회

            **정렬 옵션:**
            - latest (기본값): 최신순 (ID 내림차순)
            - name: 이름순 (오름차순) → 동일 이름은 최신순

            **조합 예시:**
            - ?regions=강남구&name=카페&sort=name
            - ?name=민트 초코&sort=latest
            - ?regions=강남구&regions=서초구&name=레스토랑&sort=name
            """
    )
    @GetMapping("/search")
    public ApiResponse<List<StoreResponse.SearchResult>> searchStores(
            @Parameter(
                description = "지역 이름 (다중 선택 가능)",
                example = "강남구"
            )
            @RequestParam(required = false) List<String> regions,

            @Parameter(
                description = "검색할 가게 이름 (공백 있으면 OR 검색, 공백 없으면 전체 검색)",
                example = "민트 초코"
            )
            @RequestParam(required = false) String name,

            @Parameter(
                description = "정렬 기준 (latest: 최신순, name: 이름순)",
                example = "latest"
            )
            @RequestParam(defaultValue = "latest") String sort
    ) {
        List<StoreResponse.SearchResult> results = storeQueryService.searchStores(regions, name, sort);
        return ApiResponse.onSuccess(SuccessCode.OK, results);
    }

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
    public ApiResponse<List<ReviewResponse.MyReview>> getStoreReviews(
            @Parameter(description = "가게 ID", required = true, example = "1")
            @PathVariable Long storeId,

            @Parameter(description = "회원 ID (특정 회원의 리뷰만 조회)", example = "1")
            @RequestParam(required = false) Long memberId,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam(required = false) Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam(required = false) Float maxScore
    ) {
        List<ReviewResponse.MyReview> reviews = reviewQueryService.getReviews(
                memberId, storeId, null, minScore, maxScore
        );
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }
}
