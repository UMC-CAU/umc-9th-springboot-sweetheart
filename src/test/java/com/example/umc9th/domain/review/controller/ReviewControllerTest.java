package com.example.umc9th.domain.review.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.domain.review.service.ReviewCommandService;
import com.example.umc9th.global.exception.GlobalExceptionHandler;
import com.example.umc9th.global.validation.validator.StoreExistValidator;
import com.example.umc9th.global.validation.validator.MemberExistValidator;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.domain.member.repository.MemberRepository;
import org.springframework.context.annotation.Import;

/**
 * ReviewController 테스트
 *
 * @WebMvcTest:
 * - Controller 계층만 테스트
 * - MockMvc를 사용해서 실제 HTTP 요청/응답 테스트
 * - Service는 @MockitoBean으로 Mock 처리
 *
 * 테스트 범위:
 * - GET /api/reviews (통합 리뷰 조회 API)
 * - 파라미터: memberId, storeId, storeName, minScore, maxScore
 *
 * 테스트 케이스:
 * 1. 전체 리뷰 조회 (파라미터 없음)
 * 2. 회원별 리뷰 조회 (memberId만)
 * 3. 가게별 리뷰 조회 (storeId만)
 * 4. 가게 이름 검색 (storeName만)
 * 5. 별점 범위 필터링 (minScore, maxScore)
 * 6. 복합 조건 (여러 파라미터 조합)
 * 7. 별점 범위 검증 실패 (minScore < 0, maxScore > 5, minScore > maxScore)
 * 8. 빈 결과 처리
 */
@WebMvcTest(controllers = {
        ReviewController.class,
        GlobalExceptionHandler.class  // 예외 처리 테스트를 위해 포함
})
@Import({
        StoreExistValidator.class,  // @ExistStore 검증을 위한 Validator
        MemberExistValidator.class   // @ExistMember 검증을 위한 Validator
})
@DisplayName("ReviewController 테스트")
class ReviewControllerTest {

    /**
     * MockMvc: HTTP 요청을 시뮬레이션하는 객체
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ReviewQueryService Mock
     * - ReviewController가 ReviewQueryService를 주입받으므로 필요
     * - @WebMvcTest는 Web 계층만 로드하므로 Service는 Mock으로 처리
     */
    @MockitoBean
    private ReviewQueryService reviewQueryService;

    /**
     * ReviewCommandService Mock
     * - ReviewController의 POST 엔드포인트에서 사용
     */
    @MockitoBean
    private ReviewCommandService reviewCommandService;

    /**
     * DiscordWebhookService Mock
     * - GlobalExceptionHandler가 DiscordWebhookService를 주입받으므로 필요
     */
    @MockitoBean
    private com.example.umc9th.global.notification.DiscordWebhookService discordWebhookService;

    /**
     * StoreRepository Mock
     * - StoreExistValidator가 StoreRepository를 주입받으므로 필요
     */
    @MockitoBean
    private StoreRepository storeRepository;

    /**
     * MemberRepository Mock
     * - MemberExistValidator가 MemberRepository를 주입받으므로 필요
     */
    @MockitoBean
    private MemberRepository memberRepository;


    // ===== GET /api/reviews - 통합 리뷰 조회 =====

    /**
     * 전체 리뷰 조회 (파라미터 없음)
     */
    @Test
    @DisplayName("GET /api/reviews - 파라미터 없이 전체 리뷰를 조회할 수 있다")
    void getReviews_All_Success() throws Exception {
        // Given: Service가 전체 리뷰 목록을 반환
        List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                createMockReview(1L, "맛있어요!", 4.5f, "반이학생", "서울시"),
                createMockReview(2L, "좋았습니다", 4.0f, "맘스터치", "경기도"),
                createMockReview(3L, "최고예요", 5.0f, "KFC", "부산시")
        );

        // getReviews(null, null, null, null, null) 호출 시 mockResponse 반환
        given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), isNull(), isNull()))
                .willReturn(mockResponse);

        // When & Then: GET 요청 실행 및 검증
        mockMvc.perform(
                        get("/api/reviews")  // GET /api/reviews (파라미터 없음)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())  // 요청/응답 로그 출력 (디버깅용)
                .andExpect(status().isOk())  // HTTP 200 OK
                .andExpect(jsonPath("$.isSuccess").value(true))  // ApiResponse.isSuccess
                .andExpect(jsonPath("$.code").value("COMMON_200"))  // SuccessCode.OK
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data").isArray())  // data는 배열
                .andExpect(jsonPath("$.data", hasSize(3)))  // 3개
                .andExpect(jsonPath("$.data[0].content").value("맛있어요!"))
                .andExpect(jsonPath("$.data[0].star").value(4.5))
                .andExpect(jsonPath("$.data[0].store.name").value("반이학생"))
                .andExpect(jsonPath("$.data[1].star").value(4.0))
                .andExpect(jsonPath("$.data[2].star").value(5.0));

        // Service 메서드 호출 확인
        then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), isNull(), isNull());
    }


    /**
     * 회원별 리뷰 조회 테스트
     */
    @Nested
    @DisplayName("회원별 리뷰 조회")
    class MemberReviews {

        @Test
        @DisplayName("GET /api/reviews?memberId=1 - 특정 회원의 리뷰를 조회할 수 있다")
        void getReviews_ByMemberId_Success() throws Exception {
            // Given: 회원 ID로 필터링된 리뷰 목록
            Long memberId = 1L;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(10L, "회원1의 리뷰1", 4.5f, "반이학생", "서울시"),
                    createMockReview(11L, "회원1의 리뷰2", 3.0f, "맘스터치", "경기도")
            );

            given(reviewQueryService.getReviews(eq(memberId), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(mockResponse);

            // When & Then: memberId 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("memberId", memberId.toString())  // ?memberId=1
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].content").value("회원1의 리뷰1"))
                    .andExpect(jsonPath("$.data[1].content").value("회원1의 리뷰2"));

            then(reviewQueryService).should().getReviews(eq(memberId), isNull(), isNull(), isNull(), isNull());
        }
    }


    /**
     * 가게별 리뷰 조회 테스트
     */
    @Nested
    @DisplayName("가게별 리뷰 조회")
    class StoreReviews {

        @Test
        @DisplayName("GET /api/reviews?storeId=5 - 특정 가게의 리뷰를 조회할 수 있다")
        void getReviews_ByStoreId_Success() throws Exception {
            // Given: 가게 ID로 필터링된 리뷰 목록
            Long storeId = 5L;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(20L, "가게 리뷰1", 5.0f, "반이학생", "서울시"),
                    createMockReview(21L, "가게 리뷰2", 4.0f, "반이학생", "서울시"),
                    createMockReview(22L, "가게 리뷰3", 3.5f, "반이학생", "서울시")
            );

            given(reviewQueryService.getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull()))
                    .willReturn(mockResponse);

            // When & Then: storeId 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("storeId", storeId.toString())  // ?storeId=5
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].star").value(5.0))
                    .andExpect(jsonPath("$.data[1].star").value(4.0))
                    .andExpect(jsonPath("$.data[2].star").value(3.5));

            then(reviewQueryService).should().getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?storeName=반이학생 - 가게 이름으로 리뷰를 검색할 수 있다")
        void getReviews_ByStoreName_Success() throws Exception {
            // Given: 가게 이름으로 검색된 리뷰 목록
            String storeName = "반이학생";
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(30L, "반이학생 강남점 후기", 4.5f, "반이학생 강남점", "서울시"),
                    createMockReview(31L, "반이학생 신촌점 후기", 4.0f, "반이학생 신촌점", "서울시")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), eq(storeName), isNull(), isNull()))
                    .willReturn(mockResponse);

            // When & Then: storeName 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("storeName", storeName)  // ?storeName=반이학생
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].store.name").value("반이학생 강남점"))
                    .andExpect(jsonPath("$.data[1].store.name").value("반이학생 신촌점"));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), eq(storeName), isNull(), isNull());
        }
    }


    /**
     * 별점 필터링 테스트
     */
    @Nested
    @DisplayName("별점 범위 필터링")
    class ScoreFiltering {

        @Test
        @DisplayName("GET /api/reviews?minScore=4.0 - 최소 별점으로 필터링할 수 있다")
        void getReviews_ByMinScore_Success() throws Exception {
            // Given: 별점 4.0 이상인 리뷰
            Float minScore = 4.0f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(40L, "별점 높은 리뷰1", 4.0f, "반이학생", "서울시"),
                    createMockReview(41L, "별점 높은 리뷰2", 4.5f, "맘스터치", "경기도"),
                    createMockReview(42L, "별점 높은 리뷰3", 5.0f, "KFC", "부산시")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(minScore), isNull()))
                    .willReturn(mockResponse);

            // When & Then: minScore 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", minScore.toString())  // ?minScore=4.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].star").value(4.0))
                    .andExpect(jsonPath("$.data[1].star").value(4.5))
                    .andExpect(jsonPath("$.data[2].star").value(5.0));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(minScore), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?maxScore=4.0 - 최대 별점으로 필터링할 수 있다")
        void getReviews_ByMaxScore_Success() throws Exception {
            // Given: 별점 4.0 이하인 리뷰
            Float maxScore = 4.0f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(50L, "별점 낮은 리뷰1", 3.0f, "반이학생", "서울시"),
                    createMockReview(51L, "별점 낮은 리뷰2", 3.5f, "맘스터치", "경기도"),
                    createMockReview(52L, "별점 낮은 리뷰3", 4.0f, "KFC", "부산시")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), isNull(), eq(maxScore)))
                    .willReturn(mockResponse);

            // When & Then: maxScore 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("maxScore", maxScore.toString())  // ?maxScore=4.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].star").value(3.0))
                    .andExpect(jsonPath("$.data[1].star").value(3.5))
                    .andExpect(jsonPath("$.data[2].star").value(4.0));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), isNull(), eq(maxScore));
        }

        @Test
        @DisplayName("GET /api/reviews?minScore=4.0&maxScore=4.99 - 별점 범위로 필터링할 수 있다")
        void getReviews_ByScoreRange_Success() throws Exception {
            // Given: 별점 4.0 ~ 4.99 범위 리뷰
            Float minScore = 4.0f;
            Float maxScore = 4.99f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(60L, "별점 4점대 리뷰1", 4.0f, "반이학생", "서울시"),
                    createMockReview(61L, "별점 4점대 리뷰2", 4.5f, "맘스터치", "경기도")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(minScore), eq(maxScore)))
                    .willReturn(mockResponse);

            // When & Then: minScore, maxScore 파라미터로 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", minScore.toString())
                                    .param("maxScore", maxScore.toString())  // ?minScore=4.0&maxScore=4.99
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].star").value(4.0))
                    .andExpect(jsonPath("$.data[1].star").value(4.5));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(minScore), eq(maxScore));
        }
    }


    /**
     * 복합 조건 테스트
     */
    @Nested
    @DisplayName("복합 조건 조회")
    class ComplexFiltering {

        @Test
        @DisplayName("GET /api/reviews?memberId=1&minScore=4.0 - 회원 + 최소 별점 조합")
        void getReviews_MemberIdAndMinScore_Success() throws Exception {
            // Given: 회원 1의 별점 4.0 이상 리뷰
            Long memberId = 1L;
            Float minScore = 4.0f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(70L, "복합 조건 리뷰", 4.5f, "반이학생", "서울시")
            );

            given(reviewQueryService.getReviews(eq(memberId), isNull(), isNull(), eq(minScore), isNull()))
                    .willReturn(mockResponse);

            // When & Then: 복합 조건 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("memberId", memberId.toString())
                                    .param("minScore", minScore.toString())  // ?memberId=1&minScore=4.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].star").value(4.5));

            then(reviewQueryService).should().getReviews(eq(memberId), isNull(), isNull(), eq(minScore), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?storeId=5&minScore=4.0&maxScore=4.5 - 가게 + 별점 범위 조합")
        void getReviews_StoreIdAndScoreRange_Success() throws Exception {
            // Given: 가게 5의 별점 4.0~4.5 리뷰
            Long storeId = 5L;
            Float minScore = 4.0f;
            Float maxScore = 4.5f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(80L, "가게 별점 필터", 4.2f, "반이학생", "서울시"),
                    createMockReview(81L, "가게 별점 필터2", 4.5f, "반이학생", "서울시")
            );

            given(reviewQueryService.getReviews(isNull(), eq(storeId), isNull(), eq(minScore), eq(maxScore)))
                    .willReturn(mockResponse);

            // When & Then: 복합 조건 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("storeId", storeId.toString())
                                    .param("minScore", minScore.toString())
                                    .param("maxScore", maxScore.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            then(reviewQueryService).should().getReviews(isNull(), eq(storeId), isNull(), eq(minScore), eq(maxScore));
        }

        @Test
        @DisplayName("GET /api/reviews?storeName=반이학생&minScore=4.0 - 가게 이름 + 최소 별점 조합")
        void getReviews_StoreNameAndMinScore_Success() throws Exception {
            // Given: 가게 이름 '반이학생' + 별점 4.0 이상
            String storeName = "반이학생";
            Float minScore = 4.0f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(90L, "이름 검색 + 별점", 4.5f, "반이학생", "서울시")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), eq(storeName), eq(minScore), isNull()))
                    .willReturn(mockResponse);

            // When & Then: 복합 조건 요청
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("storeName", storeName)
                                    .param("minScore", minScore.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), eq(storeName), eq(minScore), isNull());
        }
    }


    /**
     * 별점 범위 검증 실패 테스트
     * Service에서 IllegalArgumentException을 던지면
     * GlobalExceptionHandler가 잡아서 400 에러로 변환
     */
    @Nested
    @DisplayName("별점 범위 검증 실패")
    class ScoreValidationFailed {

        @Test
        @DisplayName("GET /api/reviews?minScore=-1 - 최소 별점이 0 미만이면 400 에러")
        void getReviews_MinScoreNegative_BadRequest() throws Exception {
            // Given: Service가 IllegalArgumentException 던짐
            Float invalidMinScore = -1.0f;
            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(invalidMinScore), isNull()))
                    .willThrow(new IllegalArgumentException("최소 별점은 0.0 이상 5.0 이하여야 합니다"));

            // When & Then: 400 에러 응답 확인
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", invalidMinScore.toString())  // ?minScore=-1
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())  // HTTP 400
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.message").value("최소 별점은 0.0 이상 5.0 이하여야 합니다"));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(invalidMinScore), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?maxScore=6.0 - 최대 별점이 5 초과면 400 에러")
        void getReviews_MaxScoreExceedsMax_BadRequest() throws Exception {
            // Given: Service가 IllegalArgumentException 던짐
            Float invalidMaxScore = 6.0f;
            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), isNull(), eq(invalidMaxScore)))
                    .willThrow(new IllegalArgumentException("최대 별점은 0.0 이상 5.0 이하여야 합니다"));

            // When & Then: 400 에러
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("maxScore", invalidMaxScore.toString())  // ?maxScore=6.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.message").value("최대 별점은 0.0 이상 5.0 이하여야 합니다"));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), isNull(), eq(invalidMaxScore));
        }

        @Test
        @DisplayName("GET /api/reviews?minScore=4.5&maxScore=3.0 - 최소 별점이 최대 별점보다 크면 400 에러")
        void getReviews_MinScoreGreaterThanMaxScore_BadRequest() throws Exception {
            // Given: Service가 IllegalArgumentException 던짐
            Float minScore = 4.5f;
            Float maxScore = 3.0f;
            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(minScore), eq(maxScore)))
                    .willThrow(new IllegalArgumentException("최소 별점은 최대 별점보다 클 수 없습니다"));

            // When & Then: 400 에러
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", minScore.toString())
                                    .param("maxScore", maxScore.toString())  // ?minScore=4.5&maxScore=3.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.message").value("최소 별점은 최대 별점보다 클 수 없습니다"));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(minScore), eq(maxScore));
        }

        @Test
        @DisplayName("GET /api/reviews?minScore=5.1 - 최소 별점이 5 초과면 400 에러")
        void getReviews_MinScoreExceedsMax_BadRequest() throws Exception {
            // Given: Service가 IllegalArgumentException 던짐
            Float invalidMinScore = 5.1f;
            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(invalidMinScore), isNull()))
                    .willThrow(new IllegalArgumentException("최소 별점은 0.0 이상 5.0 이하여야 합니다"));

            // When & Then: 400 에러
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", invalidMinScore.toString())  // ?minScore=5.1
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.message").value("최소 별점은 0.0 이상 5.0 이하여야 합니다"));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(invalidMinScore), isNull());
        }
    }


    /**
     * 빈 결과 처리 테스트
     */
    @Nested
    @DisplayName("빈 결과 처리")
    class EmptyResults {

        @Test
        @DisplayName("GET /api/reviews?memberId=999 - 해당하는 리뷰가 없으면 빈 배열 반환")
        void getReviews_NoResults_EmptyArray() throws Exception {
            // Given: Service가 빈 리스트 반환
            Long nonExistentMemberId = 999L;
            given(reviewQueryService.getReviews(eq(nonExistentMemberId), isNull(), isNull(), isNull(), isNull()))
                    .willReturn(Collections.emptyList());

            // When & Then: 성공 응답이지만 data는 빈 배열
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("memberId", nonExistentMemberId.toString())  // ?memberId=999
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())  // HTTP 200 (에러가 아님)
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));  // 빈 배열

            then(reviewQueryService).should().getReviews(eq(nonExistentMemberId), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?storeName=존재하지않는가게 - 검색 결과가 없으면 빈 배열 반환")
        void getReviews_StoreNameNoMatch_EmptyArray() throws Exception {
            // Given: Service가 빈 리스트 반환
            String nonExistentStoreName = "존재하지않는가게";
            given(reviewQueryService.getReviews(isNull(), isNull(), eq(nonExistentStoreName), isNull(), isNull()))
                    .willReturn(Collections.emptyList());

            // When & Then: 빈 배열 반환
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("storeName", nonExistentStoreName)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), eq(nonExistentStoreName), isNull(), isNull());
        }

        @Test
        @DisplayName("GET /api/reviews?minScore=5.0&maxScore=5.0 - 별점 5.0 정확히만 조회")
        void getReviews_ExactScore_Success() throws Exception {
            // Given: 별점 5.0인 리뷰만 반환
            Float exactScore = 5.0f;
            List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                    createMockReview(100L, "완벽한 리뷰", 5.0f, "반이학생", "서울시")
            );

            given(reviewQueryService.getReviews(isNull(), isNull(), isNull(), eq(exactScore), eq(exactScore)))
                    .willReturn(mockResponse);

            // When & Then: 별점 5.0 정확히 조회
            mockMvc.perform(
                            get("/api/reviews")
                                    .param("minScore", exactScore.toString())
                                    .param("maxScore", exactScore.toString())  // ?minScore=5.0&maxScore=5.0
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].star").value(5.0));

            then(reviewQueryService).should().getReviews(isNull(), isNull(), isNull(), eq(exactScore), eq(exactScore));
        }
    }


    // ===== Helper Methods =====

    /**
     * 테스트용 Mock 리뷰 생성 헬퍼 메서드
     *
     * @param id 리뷰 ID
     * @param content 리뷰 내용
     * @param star 별점
     * @param storeName 가게 이름
     * @param address 가게 주소
     * @return Mock 리뷰 응답 DTO
     */
    private ReviewResponse.MyReview createMockReview(Long id, String content, Float star, String storeName, String address) {
        return ReviewResponse.MyReview.builder()
                .id(id)
                .content(content)
                .star(star)
                .createdAt(LocalDateTime.now())
                .store(ReviewResponse.StoreInfo.builder()
                        .id(1L)
                        .name(storeName)
                        .address(address)
                        .build())
                .build();
    }
}
