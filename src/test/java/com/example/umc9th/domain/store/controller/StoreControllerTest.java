package com.example.umc9th.domain.store.controller;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.domain.store.dto.StoreResponse;
import com.example.umc9th.domain.store.service.StoreQueryService;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.exception.GlobalExceptionHandler;
import com.example.umc9th.global.response.code.ErrorCode;

/**
 * StoreController 테스트
 *
 * @WebMvcTest:
 * - Controller 계층만 테스트
 * - MockMvc를 사용해서 실제 HTTP 요청/응답 테스트
 * - Service는 @MockitoBean으로 Mock 처리
 *
 * 테스트 범위:
 * 1. GET /api/stores/search (가게 검색 API)
 *    - 파라미터: regions (List), name, sort
 *    - 총 11개 테스트 케이스
 * 2. GET /api/stores/{storeId}/reviews (가게 리뷰 조회 API)
 *    - 파라미터: memberId, minScore, maxScore
 *    - 총 6개 테스트 케이스
 *
 * 테스트 패턴:
 * - Given-When-Then 구조
 * - BDD 스타일 (given, then)
 * - @Nested 클래스로 그룹화
 * - JSON 응답 검증 (jsonPath)
 * - Service 메서드 호출 검증 (verify)
 *
 * 왜 Controller 테스트가 중요할까?
 * - API 스펙 검증 (URL, HTTP 메서드, 요청/응답 형식)
 * - 쿼리 파라미터 처리 검증 (필수/선택, 다중 파라미터)
 * - GlobalExceptionHandler 동작 확인
 * - 프론트엔드 입장에서 API 검증
 */
@WebMvcTest(controllers = {
        StoreController.class,
        GlobalExceptionHandler.class  // 예외 처리 테스트를 위해 포함
})
@DisplayName("StoreController 테스트")
class StoreControllerTest {

    /**
     * MockMvc: HTTP 요청을 시뮬레이션하는 객체
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * @MockitoBean: Spring Context에 Mock 객체 주입
     * - StoreController가 의존하는 Service를 Mock으로 대체
     *
     * 참고: Spring Boot 3.4.0+부터 @MockBean 대신 @MockitoBean 사용
     */
    @MockitoBean
    private StoreQueryService storeQueryService;

    /**
     * ReviewQueryService Mock
     * - StoreController가 ReviewQueryService를 주입받으므로 필요
     * - GET /api/stores/{storeId}/reviews 엔드포인트에서 사용
     */
    @MockitoBean
    private ReviewQueryService reviewQueryService;

    /**
     * DiscordWebhookService Mock
     * - GlobalExceptionHandler가 DiscordWebhookService를 주입받으므로 필요
     * - @WebMvcTest는 Web 계층만 로드하므로 Service는 Mock으로 처리
     */
    @MockitoBean
    private com.example.umc9th.global.notification.DiscordWebhookService discordWebhookService;


    // ===== GET /api/stores/search - 가게 검색 (11개 테스트) =====

    /**
     * 전체 가게 조회 (필터 없음)
     * - regions, name 파라미터 모두 없이 요청
     * - 모든 가게가 조회되어야 함
     */
    @Test
    @DisplayName("GET /api/stores/search - 파라미터 없이 전체 가게를 조회할 수 있다")
    void searchStores_All_Success() throws Exception {
        // Given: Service가 전체 가게 목록을 반환한다고 가정
        List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                createMockStore(1L, "반이학생", "강남구", "KOREAN", "강남대로 123"),
                createMockStore(2L, "맘스터치", "서초구", "WESTERN", "서초대로 456"),
                createMockStore(3L, "KFC", "송파구", "WESTERN", "송파대로 789")
        );

        // regions=null, name=null, sort="latest" (기본값)
        given(storeQueryService.searchStores(isNull(), isNull(), eq("latest")))
                .willReturn(mockResponse);

        // When & Then: GET 요청 실행 및 검증
        mockMvc.perform(
                        get("/api/stores/search")  // 파라미터 없음
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())  // 요청/응답 로그 출력 (디버깅용)
                .andExpect(status().isOk())  // HTTP 200 OK
                .andExpect(jsonPath("$.isSuccess").value(true))  // ApiResponse.isSuccess
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data").isArray())  // data는 배열
                .andExpect(jsonPath("$.data", hasSize(3)))  // 3개
                .andExpect(jsonPath("$.data[0].name").value("반이학생"))
                .andExpect(jsonPath("$.data[0].region").value("강남구"))
                .andExpect(jsonPath("$.data[1].name").value("맘스터치"))
                .andExpect(jsonPath("$.data[2].name").value("KFC"));

        // Service 메서드 호출 확인
        then(storeQueryService).should().searchStores(isNull(), isNull(), eq("latest"));
    }


    /**
     * 가게 검색 - 지역 필터링 테스트 (3개)
     */
    @Nested
    @DisplayName("가게 검색 - 지역 필터링")
    class RegionFiltering {

        @Test
        @DisplayName("GET /api/stores/search?regions=강남구 - 단일 지역으로 필터링할 수 있다")
        void searchStores_SingleRegion_Success() throws Exception {
            // Given: 강남구 지역 가게 목록
            List<String> regions = Arrays.asList("강남구");
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "반이학생", "강남구", "KOREAN", "강남대로 123"),
                    createMockStore(2L, "강남카페", "강남구", "ETC", "강남대로 456")
            );

            given(storeQueryService.searchStores(eq(regions), isNull(), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: regions 파라미터로 요청
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("regions", "강남구")  // ?regions=강남구
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].region").value("강남구"))
                    .andExpect(jsonPath("$.data[1].region").value("강남구"));

            then(storeQueryService).should().searchStores(eq(regions), isNull(), eq("latest"));
        }

        @Test
        @DisplayName("GET /api/stores/search?regions=강남구&regions=서초구 - 다중 지역으로 필터링할 수 있다")
        void searchStores_MultipleRegions_Success() throws Exception {
            // Given: 강남구 + 서초구 지역 가게 목록
            List<String> regions = Arrays.asList("강남구", "서초구");
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "반이학생", "강남구", "KOREAN", "강남대로 123"),
                    createMockStore(2L, "맘스터치", "서초구", "WESTERN", "서초대로 456"),
                    createMockStore(3L, "서초카페", "서초구", "ETC", "서초대로 789")
            );

            given(storeQueryService.searchStores(eq(regions), isNull(), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: 다중 regions 파라미터로 요청
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("regions", "강남구", "서초구")  // ?regions=강남구&regions=서초구
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].region").value("강남구"))
                    .andExpect(jsonPath("$.data[1].region").value("서초구"))
                    .andExpect(jsonPath("$.data[2].region").value("서초구"));

            then(storeQueryService).should().searchStores(eq(regions), isNull(), eq("latest"));
        }

        @Test
        @DisplayName("GET /api/stores/search?regions= - 빈 지역 리스트는 전체 조회로 처리된다")
        void searchStores_EmptyRegionList_Success() throws Exception {
            // Given: 빈 리스트가 전달되면 Service가 전체 조회
            List<String> emptyRegions = Collections.emptyList();
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "반이학생", "강남구", "KOREAN", "강남대로 123"),
                    createMockStore(2L, "맘스터치", "서초구", "WESTERN", "서초대로 456")
            );

            given(storeQueryService.searchStores(eq(emptyRegions), isNull(), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: 빈 regions 파라미터
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("regions", "")  // ?regions= (빈 값)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            then(storeQueryService).should().searchStores(eq(emptyRegions), isNull(), eq("latest"));
        }
    }


    /**
     * 가게 검색 - 이름 검색 테스트 (2개)
     */
    @Nested
    @DisplayName("가게 검색 - 이름 검색")
    class NameSearch {

        @Test
        @DisplayName("GET /api/stores/search?name=카페 - 이름으로 검색할 수 있다")
        void searchStores_ByName_Success() throws Exception {
            // Given: 이름에 '카페'가 포함된 가게 목록
            String searchName = "카페";
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "강남카페", "강남구", "ETC", "강남대로 123"),
                    createMockStore(2L, "서초카페", "서초구", "ETC", "서초대로 456")
            );

            given(storeQueryService.searchStores(isNull(), eq(searchName), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: name 파라미터로 요청
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("name", searchName)  // ?name=카페
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name").value("강남카페"))
                    .andExpect(jsonPath("$.data[1].name").value("서초카페"));

            then(storeQueryService).should().searchStores(isNull(), eq(searchName), eq("latest"));
        }

        @Test
        @DisplayName("GET /api/stores/search?name=민트 초코 - 공백 포함 검색 (OR 조건)")
        void searchStores_ByNameWithSpaces_Success() throws Exception {
            // Given: '민트' 또는 '초코'가 포함된 가게 목록 (OR 검색)
            String searchName = "민트 초코";
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "민트카페", "강남구", "ETC", "강남대로 123"),
                    createMockStore(2L, "초코레스토랑", "서초구", "WESTERN", "서초대로 456"),
                    createMockStore(3L, "민트초코디저트", "송파구", "ETC", "송파대로 789")
            );

            given(storeQueryService.searchStores(isNull(), eq(searchName), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: 공백 포함 name 파라미터
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("name", searchName)  // ?name=민트 초코
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)));

            then(storeQueryService).should().searchStores(isNull(), eq(searchName), eq("latest"));
        }
    }


    /**
     * 가게 검색 - 정렬 테스트 (3개)
     */
    @Nested
    @DisplayName("가게 검색 - 정렬 옵션")
    class SortOptions {

        @Test
        @DisplayName("GET /api/stores/search?sort=latest - 최신순으로 정렬할 수 있다")
        void searchStores_SortByLatest_Success() throws Exception {
            // Given: 최신순 정렬된 가게 목록 (ID 내림차순)
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(3L, "최신가게", "강남구", "KOREAN", "강남대로 123"),
                    createMockStore(2L, "중간가게", "서초구", "WESTERN", "서초대로 456"),
                    createMockStore(1L, "오래된가게", "송파구", "ETC", "송파대로 789")
            );

            given(storeQueryService.searchStores(isNull(), isNull(), eq("latest")))
                    .willReturn(mockResponse);

            // When & Then: sort=latest 파라미터
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("sort", "latest")  // ?sort=latest
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].id").value(3))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[2].id").value(1));

            then(storeQueryService).should().searchStores(isNull(), isNull(), eq("latest"));
        }

        @Test
        @DisplayName("GET /api/stores/search?sort=name - 이름순으로 정렬할 수 있다")
        void searchStores_SortByName_Success() throws Exception {
            // Given: 이름순 정렬된 가게 목록 (이름 오름차순)
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "가나다가게", "강남구", "KOREAN", "강남대로 123"),
                    createMockStore(2L, "나다라가게", "서초구", "WESTERN", "서초대로 456"),
                    createMockStore(3L, "다라마가게", "송파구", "ETC", "송파대로 789")
            );

            given(storeQueryService.searchStores(isNull(), isNull(), eq("name")))
                    .willReturn(mockResponse);

            // When & Then: sort=name 파라미터
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("sort", "name")  // ?sort=name
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].name").value("가나다가게"))
                    .andExpect(jsonPath("$.data[1].name").value("나다라가게"))
                    .andExpect(jsonPath("$.data[2].name").value("다라마가게"));

            then(storeQueryService).should().searchStores(isNull(), isNull(), eq("name"));
        }

        @Test
        @DisplayName("GET /api/stores/search?sort=invalid - 잘못된 정렬 옵션은 Service로 전달되어 처리된다")
        void searchStores_InvalidSortOption_PassedToService() throws Exception {
            // Given: Service가 잘못된 정렬 옵션을 받아도 처리 (기본값 적용 등)
            List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                    createMockStore(1L, "가게1", "강남구", "KOREAN", "주소1")
            );

            // Service에서 invalid를 그대로 받아서 처리
            given(storeQueryService.searchStores(isNull(), isNull(), eq("invalid")))
                    .willReturn(mockResponse);

            // When & Then: sort=invalid 파라미터로 요청
            mockMvc.perform(
                            get("/api/stores/search")
                                    .param("sort", "invalid")  // ?sort=invalid
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())  // 에러가 아닌 정상 응답
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            then(storeQueryService).should().searchStores(isNull(), isNull(), eq("invalid"));
        }
    }


    /**
     * 가게 검색 - 복합 조건 테스트 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/search?regions=강남구&name=카페&sort=name - 지역 + 이름 + 정렬 조합")
    void searchStores_ComplexConditions_Success() throws Exception {
        // Given: 강남구 + 이름에 '카페' + 이름순 정렬
        List<String> regions = Arrays.asList("강남구");
        String searchName = "카페";
        String sort = "name";
        List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                createMockStore(1L, "강남카페A", "강남구", "ETC", "강남대로 123"),
                createMockStore(2L, "강남카페B", "강남구", "ETC", "강남대로 456")
        );

        given(storeQueryService.searchStores(eq(regions), eq(searchName), eq(sort)))
                .willReturn(mockResponse);

        // When & Then: 복합 조건 요청
        mockMvc.perform(
                        get("/api/stores/search")
                                .param("regions", "강남구")
                                .param("name", searchName)
                                .param("sort", sort)  // ?regions=강남구&name=카페&sort=name
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].region").value("강남구"))
                .andExpect(jsonPath("$.data[1].region").value("강남구"));

        then(storeQueryService).should().searchStores(eq(regions), eq(searchName), eq(sort));
    }


    /**
     * 가게 검색 - 빈 결과 처리 테스트 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/search?name=존재하지않는가게 - 검색 결과가 없으면 빈 배열 반환")
    void searchStores_NoResults_EmptyArray() throws Exception {
        // Given: Service가 빈 리스트 반환
        String nonExistentName = "존재하지않는가게";
        given(storeQueryService.searchStores(isNull(), eq(nonExistentName), eq("latest")))
                .willReturn(Collections.emptyList());

        // When & Then: 성공 응답이지만 data는 빈 배열
        mockMvc.perform(
                        get("/api/stores/search")
                                .param("name", nonExistentName)  // ?name=존재하지않는가게
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())  // HTTP 200 (에러가 아님)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));  // 빈 배열

        then(storeQueryService).should().searchStores(isNull(), eq(nonExistentName), eq("latest"));
    }


    /**
     * 가게 검색 - 응답 구조 검증 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/search - 응답 구조가 올바르게 반환된다 ($.isSuccess, $.code, $.data)")
    void searchStores_ResponseStructure_Valid() throws Exception {
        // Given: 응답 구조 검증을 위한 가게 목록
        List<StoreResponse.SearchResult> mockResponse = Arrays.asList(
                createMockStore(1L, "반이학생", "강남구", "KOREAN", "강남대로 123")
        );

        given(storeQueryService.searchStores(isNull(), isNull(), eq("latest")))
                .willReturn(mockResponse);

        // When & Then: ApiResponse 구조 검증
        mockMvc.perform(
                        get("/api/stores/search")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                // ApiResponse 구조 검증
                .andExpect(jsonPath("$.isSuccess").exists())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isArray())
                // SearchResult 구조 검증
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].region").exists())
                .andExpect(jsonPath("$.data[0].foodCategory").exists())
                .andExpect(jsonPath("$.data[0].detailAddress").exists());

        then(storeQueryService).should().searchStores(isNull(), isNull(), eq("latest"));
    }


    // ===== GET /api/stores/{storeId}/reviews - 가게 리뷰 조회 (6개 테스트) =====

    /**
     * 가게 리뷰 조회 - 전체 리뷰 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews - 특정 가게의 전체 리뷰를 조회할 수 있다")
    void getStoreReviews_All_Success() throws Exception {
        // Given: 특정 가게의 전체 리뷰 목록
        Long storeId = 1L;
        List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                createMockReview(1L, "맛있어요!", 4.5f, "반이학생"),
                createMockReview(2L, "좋았습니다", 4.0f, "반이학생"),
                createMockReview(3L, "최고예요", 5.0f, "반이학생")
        );

        // getReviews(null, storeId, null, null, null) 호출 시 mockResponse 반환
        given(reviewQueryService.getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull()))
                .willReturn(mockResponse);

        // When & Then: GET 요청 실행 및 검증
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", storeId)  // GET /api/stores/1/reviews
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())  // HTTP 200 OK
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].content").value("맛있어요!"))
                .andExpect(jsonPath("$.data[0].star").value(4.5))
                .andExpect(jsonPath("$.data[1].star").value(4.0))
                .andExpect(jsonPath("$.data[2].star").value(5.0));

        // Service 메서드 호출 확인
        then(reviewQueryService).should().getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull());
    }


    /**
     * 가게 리뷰 조회 - 회원 ID 필터링 테스트 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews?memberId=1 - 특정 회원의 리뷰만 조회할 수 있다")
    void getStoreReviews_ByMemberId_Success() throws Exception {
        // Given: 가게 1에 대한 회원 1의 리뷰
        Long storeId = 1L;
        Long memberId = 1L;
        List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                createMockReview(10L, "회원1의 리뷰", 4.5f, "반이학생")
        );

        given(reviewQueryService.getReviews(eq(memberId), eq(storeId), isNull(), isNull(), isNull()))
                .willReturn(mockResponse);

        // When & Then: memberId 파라미터로 요청
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", storeId)
                                .param("memberId", memberId.toString())  // ?memberId=1
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("회원1의 리뷰"));

        then(reviewQueryService).should().getReviews(eq(memberId), eq(storeId), isNull(), isNull(), isNull());
    }


    /**
     * 가게 리뷰 조회 - 별점 필터링 테스트 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews?minScore=4.0&maxScore=4.5 - 별점 범위로 필터링할 수 있다")
    void getStoreReviews_ByScoreRange_Success() throws Exception {
        // Given: 가게 1의 별점 4.0 ~ 4.5 범위 리뷰
        Long storeId = 1L;
        Float minScore = 4.0f;
        Float maxScore = 4.5f;
        List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                createMockReview(20L, "별점 4점대 리뷰1", 4.0f, "반이학생"),
                createMockReview(21L, "별점 4점대 리뷰2", 4.5f, "반이학생")
        );

        given(reviewQueryService.getReviews(isNull(), eq(storeId), isNull(), eq(minScore), eq(maxScore)))
                .willReturn(mockResponse);

        // When & Then: minScore, maxScore 파라미터로 요청
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", storeId)
                                .param("minScore", minScore.toString())
                                .param("maxScore", maxScore.toString())  // ?minScore=4.0&maxScore=4.5
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].star").value(4.0))
                .andExpect(jsonPath("$.data[1].star").value(4.5));

        then(reviewQueryService).should().getReviews(isNull(), eq(storeId), isNull(), eq(minScore), eq(maxScore));
    }


    /**
     * 가게 리뷰 조회 - 복합 조건 테스트 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews?memberId=1&minScore=4.0 - 회원 + 별점 조합")
    void getStoreReviews_ComplexConditions_Success() throws Exception {
        // Given: 가게 1에 대한 회원 1의 별점 4.0 이상 리뷰
        Long storeId = 1L;
        Long memberId = 1L;
        Float minScore = 4.0f;
        List<ReviewResponse.MyReview> mockResponse = Arrays.asList(
                createMockReview(40L, "복합 조건 리뷰", 4.5f, "반이학생")
        );

        given(reviewQueryService.getReviews(eq(memberId), eq(storeId), isNull(), eq(minScore), isNull()))
                .willReturn(mockResponse);

        // When & Then: 복합 조건 요청
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", storeId)
                                .param("memberId", memberId.toString())
                                .param("minScore", minScore.toString())  // ?memberId=1&minScore=4.0
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].star").value(4.5));

        then(reviewQueryService).should().getReviews(eq(memberId), eq(storeId), isNull(), eq(minScore), isNull());
    }


    /**
     * 가게 리뷰 조회 - 존재하지 않는 가게 처리 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews - 존재하지 않는 가게 ID로 조회 시 404 에러")
    void getStoreReviews_StoreNotFound_Returns404() throws Exception {
        // Given: Service가 CustomException 던짐
        Long nonExistentStoreId = 999L;
        given(reviewQueryService.getReviews(isNull(), eq(nonExistentStoreId), isNull(), isNull(), isNull()))
                .willThrow(new CustomException(ErrorCode.STORE_NOT_FOUND));

        // When & Then: 404 에러 응답 확인
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", nonExistentStoreId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())  // HTTP 404
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("STORE_404"))
                .andExpect(jsonPath("$.message").value("가게를 찾을 수 없습니다"));

        then(reviewQueryService).should().getReviews(isNull(), eq(nonExistentStoreId), isNull(), isNull(), isNull());
    }


    /**
     * 가게 리뷰 조회 - 빈 결과 처리 (1개)
     */
    @Test
    @DisplayName("GET /api/stores/{storeId}/reviews - 리뷰가 없는 가게는 빈 배열 반환")
    void getStoreReviews_NoReviews_EmptyArray() throws Exception {
        // Given: 리뷰가 없는 가게
        Long storeId = 999L;
        given(reviewQueryService.getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull()))
                .willReturn(Collections.emptyList());

        // When & Then: 성공 응답이지만 data는 빈 배열
        mockMvc.perform(
                        get("/api/stores/{storeId}/reviews", storeId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())  // HTTP 200 (에러가 아님)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));  // 빈 배열

        then(reviewQueryService).should().getReviews(isNull(), eq(storeId), isNull(), isNull(), isNull());
    }


    // ===== Helper Methods =====

    /**
     * 테스트용 Mock 가게 생성 헬퍼 메서드
     *
     * @param id 가게 ID
     * @param name 가게 이름
     * @param region 지역
     * @param foodCategory 음식 카테고리
     * @param detailAddress 상세 주소
     * @return Mock 가게 응답 DTO
     */
    private StoreResponse.SearchResult createMockStore(Long id, String name, String region,
                                                       String foodCategory, String detailAddress) {
        return StoreResponse.SearchResult.builder()
                .id(id)
                .name(name)
                .region(region)
                .foodCategory(foodCategory)
                .detailAddress(detailAddress)
                .build();
    }

    /**
     * 테스트용 Mock 리뷰 생성 헬퍼 메서드
     *
     * @param id 리뷰 ID
     * @param content 리뷰 내용
     * @param star 별점
     * @param storeName 가게 이름
     * @return Mock 리뷰 응답 DTO
     */
    private ReviewResponse.MyReview createMockReview(Long id, String content, Float star, String storeName) {
        return ReviewResponse.MyReview.builder()
                .id(id)
                .content(content)
                .star(star)
                .createdAt(LocalDateTime.now())
                .store(ReviewResponse.StoreInfo.builder()
                        .id(1L)
                        .name(storeName)
                        .address("서울시")
                        .build())
                .build();
    }
}
