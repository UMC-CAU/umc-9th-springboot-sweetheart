package com.example.umc9th.domain.store.service;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.store.dto.StoreResponse;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * StoreQueryService 테스트
 *
 * @ExtendWith(MockitoExtension.class):
 * - Mockito를 사용한 단위 테스트
 * - 실제 DB를 사용하지 않음 (Mock 객체 사용)
 * - 매우 빠르게 실행됨
 *
 * Mock vs Real:
 * - Mock: 가짜 객체, "이렇게 동작한다고 가정"
 * - Real: 실제 객체, 진짜 DB에 저장/조회
 *
 * 왜 Service는 Mock으로 테스트할까?
 * - Service는 비즈니스 로직만 테스트하고 싶음
 * - Repository가 제대로 동작한다고 가정 (Repository 테스트는 따로 함)
 * - DB 없이 빠르게 테스트 가능
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreQueryService 테스트")
class StoreQueryServiceTest {

    /**
     * @Mock: 가짜 객체 생성
     * - 실제 StoreQueryRepository가 아닌 Mock 객체
     * - given()으로 동작 방식을 정의해줘야 함
     */
    @Mock
    private StoreQueryRepository storeQueryRepository;

    /**
     * @InjectMocks: @Mock 객체들을 자동 주입
     * - StoreQueryService의 생성자에 storeQueryRepository를 자동 주입
     * - 실제로 테스트할 대상 객체
     */
    @InjectMocks
    private StoreQueryService storeQueryService;


    // ===== searchStores() 메서드 테스트 =====

    /**
     * 전체 가게 조회 테스트 (필터링 없음)
     *
     * BDD 스타일 (Behavior-Driven Development):
     * - given(): "이런 상황이 주어졌을 때"
     * - when(): "이걸 실행하면"
     * - then(): "이런 결과가 나와야 한다"
     */
    @Test
    @DisplayName("모든 가게를 조회할 수 있다 (필터링 조건이 모두 null인 경우)")
    void searchStores_AllStores_Success() {
        // Given: Repository가 모든 가게를 반환한다고 가정
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "가게A", "강남구", FoodName.KOREAN),
                createMockStore(2L, "가게B", "서초구", FoodName.JAPANESE),
                createMockStore(3L, "가게C", "강남구", FoodName.CHINESE)
        );

        // Mockito: "searchStores(null, null, null)이 호출되면 mockStores를 반환해라"
        given(storeQueryRepository.searchStores(null, null, null))
                .willReturn(mockStores);

        // When: Service의 searchStores() 호출
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, null);

        // Then: 결과 검증
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("name")
                .containsExactly("가게A", "가게B", "가게C");

        // 검증: Repository의 searchStores가 1번 호출되었는지
        then(storeQueryRepository).should(times(1))
                .searchStores(null, null, null);
    }

    /**
     * 단일 지역 필터링 테스트
     */
    @Test
    @DisplayName("특정 지역의 가게만 조회할 수 있다 (단일 지역 필터링)")
    void searchStores_SingleRegion_Success() {
        // Given: 강남구 가게만 반환
        List<String> regions = List.of("강남구");
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "강남가게A", "강남구", FoodName.KOREAN),
                createMockStore(2L, "강남가게B", "강남구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.searchStores(regions, null, null))
                .willReturn(mockStores);

        // When: 강남구 필터로 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(regions, null, null);

        // Then: 강남구 가게만 2개 조회됨
        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(store -> store.getRegion().equals("강남구"));

        then(storeQueryRepository).should().searchStores(regions, null, null);
    }

    /**
     * 다중 지역 필터링 테스트
     */
    @Test
    @DisplayName("여러 지역의 가게를 동시에 조회할 수 있다 (다중 지역 필터링)")
    void searchStores_MultipleRegions_Success() {
        // Given: 강남구, 서초구 가게 반환
        List<String> regions = Arrays.asList("강남구", "서초구");
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "강남가게", "강남구", FoodName.KOREAN),
                createMockStore(2L, "서초가게", "서초구", FoodName.JAPANESE),
                createMockStore(3L, "강남가게2", "강남구", FoodName.CHINESE)
        );

        given(storeQueryRepository.searchStores(regions, null, null))
                .willReturn(mockStores);

        // When: 강남구, 서초구 필터로 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(regions, null, null);

        // Then: 3개 조회, 강남구 또는 서초구만
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("region")
                .containsOnly("강남구", "서초구");

        then(storeQueryRepository).should().searchStores(regions, null, null);
    }

    /**
     * 빈 지역 리스트로 검색 (전체 조회와 동일)
     */
    @Test
    @DisplayName("빈 지역 리스트로 검색하면 전체 가게를 조회한다")
    void searchStores_EmptyRegionList_Success() {
        // Given: 빈 리스트로 전체 조회
        List<String> emptyRegions = Collections.emptyList();
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "가게A", "강남구", FoodName.KOREAN),
                createMockStore(2L, "가게B", "서초구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.searchStores(emptyRegions, null, null))
                .willReturn(mockStores);

        // When: 빈 리스트로 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(emptyRegions, null, null);

        // Then: 전체 가게 조회됨
        assertThat(result).hasSize(2);

        then(storeQueryRepository).should().searchStores(emptyRegions, null, null);
    }

    /**
     * 가게 이름 검색 테스트 (정상 케이스)
     */
    @Test
    @DisplayName("가게 이름으로 검색할 수 있다")
    void searchStores_ByName_Success() {
        // Given: "맛집"이라는 키워드로 검색
        String searchName = "맛집";
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "강남맛집", "강남구", FoodName.KOREAN),
                createMockStore(2L, "서초맛집", "서초구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.searchStores(null, searchName, null))
                .willReturn(mockStores);

        // When: 이름 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, searchName, null);

        // Then: "맛집"이 포함된 가게만 조회됨
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(StoreResponse.SearchResult::getName)
                .allMatch(name -> ((String) name).contains("맛집"));

        then(storeQueryRepository).should().searchStores(null, searchName, null);
    }

    /**
     * 빈 문자열로 이름 검색 (전체 조회)
     */
    @Test
    @DisplayName("빈 문자열로 이름 검색하면 전체 가게를 조회한다")
    void searchStores_EmptySearchName_Success() {
        // Given: 빈 문자열로 전체 조회
        String emptySearchName = "";
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "가게A", "강남구", FoodName.KOREAN),
                createMockStore(2L, "가게B", "서초구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.searchStores(null, emptySearchName, null))
                .willReturn(mockStores);

        // When: 빈 문자열로 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, emptySearchName, null);

        // Then: 전체 가게 조회됨
        assertThat(result).hasSize(2);

        then(storeQueryRepository).should().searchStores(null, emptySearchName, null);
    }

    /**
     * 정렬 옵션 - latest (최신순)
     */
    @Test
    @DisplayName("최신순으로 가게를 정렬할 수 있다 (sortBy=latest)")
    void searchStores_SortByLatest_Success() {
        // Given: latest 정렬 (ID 내림차순)
        String sortBy = "latest";
        List<Store> mockStores = Arrays.asList(
                createMockStore(3L, "가게C", "강남구", FoodName.KOREAN),
                createMockStore(2L, "가게B", "서초구", FoodName.JAPANESE),
                createMockStore(1L, "가게A", "강남구", FoodName.CHINESE)
        );

        given(storeQueryRepository.searchStores(null, null, sortBy))
                .willReturn(mockStores);

        // When: latest 정렬로 조회
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, sortBy);

        // Then: ID가 큰 순서대로 정렬됨
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("id")
                .containsExactly(3L, 2L, 1L);

        then(storeQueryRepository).should().searchStores(null, null, sortBy);
    }

    /**
     * 정렬 옵션 - name (이름순)
     */
    @Test
    @DisplayName("이름순으로 가게를 정렬할 수 있다 (sortBy=name)")
    void searchStores_SortByName_Success() {
        // Given: name 정렬 (이름 오름차순)
        String sortBy = "name";
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "A가게", "강남구", FoodName.KOREAN),
                createMockStore(2L, "B가게", "서초구", FoodName.JAPANESE),
                createMockStore(3L, "C가게", "강남구", FoodName.CHINESE)
        );

        given(storeQueryRepository.searchStores(null, null, sortBy))
                .willReturn(mockStores);

        // When: name 정렬로 조회
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, sortBy);

        // Then: 이름 오름차순으로 정렬됨
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("name")
                .containsExactly("A가게", "B가게", "C가게");

        then(storeQueryRepository).should().searchStores(null, null, sortBy);
    }

    /**
     * 정렬 옵션 - null (기본값: latest)
     */
    @Test
    @DisplayName("정렬 옵션이 null이면 기본값(latest)으로 정렬된다")
    void searchStores_SortByNull_UsesDefaultLatest() {
        // Given: sortBy가 null이면 기본값 latest 적용
        List<Store> mockStores = Arrays.asList(
                createMockStore(3L, "가게C", "강남구", FoodName.KOREAN),
                createMockStore(2L, "가게B", "서초구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.searchStores(null, null, null))
                .willReturn(mockStores);

        // When: sortBy 없이 조회
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, null);

        // Then: 최신순으로 정렬됨
        assertThat(result).hasSize(2);

        then(storeQueryRepository).should().searchStores(null, null, null);
    }

    /**
     * 정렬 옵션 - 잘못된 값 (Repository에서 처리, latest로 fallback)
     */
    @Test
    @DisplayName("잘못된 정렬 옵션이 입력되면 Repository가 처리한다")
    void searchStores_SortByInvalid_HandledByRepository() {
        // Given: 잘못된 sortBy 값
        String invalidSortBy = "invalid_sort";
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "가게A", "강남구", FoodName.KOREAN)
        );

        given(storeQueryRepository.searchStores(null, null, invalidSortBy))
                .willReturn(mockStores);

        // When: 잘못된 sortBy로 조회
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, invalidSortBy);

        // Then: Repository가 알아서 처리 (예외 발생 안 함)
        assertThat(result).hasSize(1);

        then(storeQueryRepository).should().searchStores(null, null, invalidSortBy);
    }

    /**
     * 복합 조건 테스트 (지역 + 이름 + 정렬)
     */
    @Test
    @DisplayName("지역 필터링, 이름 검색, 정렬을 동시에 적용할 수 있다")
    void searchStores_ComplexConditions_Success() {
        // Given: 강남구 + "카페" 키워드 + 이름순 정렬
        List<String> regions = List.of("강남구");
        String searchName = "카페";
        String sortBy = "name";
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "A카페", "강남구", FoodName.WESTERN),
                createMockStore(2L, "B카페", "강남구", FoodName.WESTERN)
        );

        given(storeQueryRepository.searchStores(regions, searchName, sortBy))
                .willReturn(mockStores);

        // When: 복합 조건으로 검색
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(regions, searchName, sortBy);

        // Then: 조건에 맞는 가게만 정렬되어 조회됨
        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(store -> store.getRegion().equals("강남구"))
                .allMatch(store -> store.getName().contains("카페"));

        then(storeQueryRepository).should().searchStores(regions, searchName, sortBy);
    }

    /**
     * 검색 결과가 없는 경우
     */
    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
    void searchStores_NoResults_ReturnsEmptyList() {
        // Given: 검색 결과 없음
        List<String> regions = List.of("존재하지않는지역");

        given(storeQueryRepository.searchStores(regions, null, null))
                .willReturn(Collections.emptyList());

        // When: 검색 실행
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(regions, null, null);

        // Then: 빈 리스트 반환 (예외 발생 X)
        assertThat(result).isEmpty();

        then(storeQueryRepository).should().searchStores(regions, null, null);
    }

    /**
     * DTO 변환 검증 테스트
     */
    @Test
    @DisplayName("Store 엔티티가 SearchResult DTO로 올바르게 변환된다")
    void searchStores_DtoConversion_Success() {
        // Given: Store 엔티티
        List<Store> mockStores = List.of(
                createMockStore(1L, "테스트가게", "강남구", FoodName.KOREAN)
        );

        given(storeQueryRepository.searchStores(null, null, null))
                .willReturn(mockStores);

        // When: 조회 실행
        List<StoreResponse.SearchResult> result = storeQueryService.searchStores(null, null, null);

        // Then: DTO 필드가 모두 정확히 변환됨
        assertThat(result).hasSize(1);
        StoreResponse.SearchResult dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("테스트가게");
        assertThat(dto.getRegion()).isEqualTo("강남구");
        assertThat(dto.getFoodCategory()).isEqualTo("KOREAN");
        assertThat(dto.getDetailAddress()).isEqualTo("강남구 테헤란로 123");
    }


    // ===== countStoresByRegion() 메서드 테스트 =====

    /**
     * 특정 지역의 가게 개수 조회 - 정상 케이스
     */
    @Test
    @DisplayName("특정 지역의 가게 개수를 조회할 수 있다")
    void countStoresByRegion_Success() {
        // Given: 강남구에 5개 가게가 있음
        String regionName = "강남구";
        Long expectedCount = 5L;

        given(storeQueryRepository.countStoresByRegion(regionName))
                .willReturn(expectedCount);

        // When: 가게 개수 조회
        Long result = storeQueryService.countStoresByRegion(regionName);

        // Then: 5개 반환
        assertThat(result).isEqualTo(5L);

        then(storeQueryRepository).should().countStoresByRegion(regionName);
    }

    /**
     * 가게가 없는 지역 조회 - 0 반환
     */
    @Test
    @DisplayName("가게가 없는 지역은 0을 반환한다")
    void countStoresByRegion_NoStores_ReturnsZero() {
        // Given: 해당 지역에 가게가 없음
        String regionName = "제주도";
        Long expectedCount = 0L;

        given(storeQueryRepository.countStoresByRegion(regionName))
                .willReturn(expectedCount);

        // When: 가게 개수 조회
        Long result = storeQueryService.countStoresByRegion(regionName);

        // Then: 0 반환 (예외 발생 X)
        assertThat(result).isEqualTo(0L);

        then(storeQueryRepository).should().countStoresByRegion(regionName);
    }

    /**
     * 여러 지역의 가게 개수 조회 (연속 호출)
     */
    @Test
    @DisplayName("여러 지역의 가게 개수를 연속으로 조회할 수 있다")
    void countStoresByRegion_MultipleRegions_Success() {
        // Given: 각 지역별 가게 개수
        given(storeQueryRepository.countStoresByRegion("강남구")).willReturn(10L);
        given(storeQueryRepository.countStoresByRegion("서초구")).willReturn(7L);
        given(storeQueryRepository.countStoresByRegion("송파구")).willReturn(5L);

        // When: 각 지역 개수 조회
        Long gangnam = storeQueryService.countStoresByRegion("강남구");
        Long seocho = storeQueryService.countStoresByRegion("서초구");
        Long songpa = storeQueryService.countStoresByRegion("송파구");

        // Then: 각각 올바른 개수 반환
        assertThat(gangnam).isEqualTo(10L);
        assertThat(seocho).isEqualTo(7L);
        assertThat(songpa).isEqualTo(5L);

        // 각각 1번씩 호출되었는지 검증
        then(storeQueryRepository).should().countStoresByRegion("강남구");
        then(storeQueryRepository).should().countStoresByRegion("서초구");
        then(storeQueryRepository).should().countStoresByRegion("송파구");
    }


    // ===== getStoresByFood() 메서드 테스트 =====

    /**
     * 특정 음식 카테고리의 가게 조회 - 정상 케이스
     */
    @Test
    @DisplayName("특정 음식 카테고리의 가게 목록을 조회할 수 있다")
    void getStoresByFood_Success() {
        // Given: 한식(KOREAN) 카테고리 ID = 1L
        Long foodId = 1L;
        List<Store> mockStores = Arrays.asList(
                createMockStore(1L, "한식당A", "강남구", FoodName.KOREAN),
                createMockStore(2L, "한식당B", "서초구", FoodName.KOREAN),
                createMockStore(3L, "한식당C", "송파구", FoodName.KOREAN)
        );

        given(storeQueryRepository.findStoresByFoodId(foodId))
                .willReturn(mockStores);

        // When: 한식 카테고리 가게 조회
        List<StoreResponse.SearchResult> result = storeQueryService.getStoresByFood(foodId);

        // Then: 한식당 3개 조회됨
        assertThat(result).hasSize(3);
        assertThat(result)
                .allMatch(store -> store.getFoodCategory().equals("KOREAN"));

        then(storeQueryRepository).should().findStoresByFoodId(foodId);
    }

    /**
     * 음식 카테고리에 가게가 없는 경우
     */
    @Test
    @DisplayName("음식 카테고리에 가게가 없으면 빈 리스트를 반환한다")
    void getStoresByFood_NoStores_ReturnsEmptyList() {
        // Given: 해당 카테고리에 가게가 없음
        Long foodId = 999L;

        given(storeQueryRepository.findStoresByFoodId(foodId))
                .willReturn(Collections.emptyList());

        // When: 조회 실행
        List<StoreResponse.SearchResult> result = storeQueryService.getStoresByFood(foodId);

        // Then: 빈 리스트 반환 (예외 발생 X)
        assertThat(result).isEmpty();

        then(storeQueryRepository).should().findStoresByFoodId(foodId);
    }

    /**
     * 여러 음식 카테고리 연속 조회
     */
    @Test
    @DisplayName("여러 음식 카테고리의 가게를 연속으로 조회할 수 있다")
    void getStoresByFood_MultipleCategories_Success() {
        // Given: 각 카테고리별 가게
        List<Store> koreanStores = List.of(
                createMockStore(1L, "한식당", "강남구", FoodName.KOREAN)
        );
        List<Store> japaneseStores = List.of(
                createMockStore(2L, "일식당", "서초구", FoodName.JAPANESE)
        );

        given(storeQueryRepository.findStoresByFoodId(1L)).willReturn(koreanStores);
        given(storeQueryRepository.findStoresByFoodId(2L)).willReturn(japaneseStores);

        // When: 각 카테고리 조회
        List<StoreResponse.SearchResult> korean = storeQueryService.getStoresByFood(1L);
        List<StoreResponse.SearchResult> japanese = storeQueryService.getStoresByFood(2L);

        // Then: 각각 올바른 카테고리 반환
        assertThat(korean).hasSize(1);
        assertThat(korean.get(0).getFoodCategory()).isEqualTo("KOREAN");

        assertThat(japanese).hasSize(1);
        assertThat(japanese.get(0).getFoodCategory()).isEqualTo("JAPANESE");

        then(storeQueryRepository).should().findStoresByFoodId(1L);
        then(storeQueryRepository).should().findStoresByFoodId(2L);
    }

    /**
     * DTO 변환 검증 (음식 카테고리 조회)
     */
    @Test
    @DisplayName("getStoresByFood의 결과가 올바르게 DTO로 변환된다")
    void getStoresByFood_DtoConversion_Success() {
        // Given: 중식 가게
        Long foodId = 3L;
        List<Store> mockStores = List.of(
                createMockStore(1L, "중식당", "강남구", FoodName.CHINESE)
        );

        given(storeQueryRepository.findStoresByFoodId(foodId))
                .willReturn(mockStores);

        // When: 조회 실행
        List<StoreResponse.SearchResult> result = storeQueryService.getStoresByFood(foodId);

        // Then: DTO 필드가 모두 정확히 변환됨
        assertThat(result).hasSize(1);
        StoreResponse.SearchResult dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("중식당");
        assertThat(dto.getRegion()).isEqualTo("강남구");
        assertThat(dto.getFoodCategory()).isEqualTo("CHINESE");
        assertThat(dto.getDetailAddress()).isEqualTo("강남구 테헤란로 123");
    }


    // ===== Repository 호출 검증 테스트 =====

    /**
     * Repository 메서드가 정확히 1번만 호출되는지 검증
     */
    @Test
    @DisplayName("Service는 Repository 메서드를 정확히 1번만 호출한다")
    void verifyRepositoryCallCount() {
        // Given
        List<Store> mockStores = List.of(createMockStore(1L, "가게", "강남구", FoodName.KOREAN));
        given(storeQueryRepository.searchStores(any(), any(), any())).willReturn(mockStores);

        // When: Service 메서드 1번 호출
        storeQueryService.searchStores(null, null, null);

        // Then: Repository도 정확히 1번만 호출됨
        then(storeQueryRepository).should(times(1)).searchStores(any(), any(), any());
        then(storeQueryRepository).shouldHaveNoMoreInteractions();
    }

    /**
     * Repository에 올바른 파라미터가 전달되는지 검증
     */
    @Test
    @DisplayName("Service는 Repository에 올바른 파라미터를 전달한다")
    void verifyRepositoryParameters() {
        // Given
        List<String> regions = List.of("강남구", "서초구");
        String searchName = "맛집";
        String sortBy = "name";
        List<Store> mockStores = Collections.emptyList();

        given(storeQueryRepository.searchStores(regions, searchName, sortBy))
                .willReturn(mockStores);

        // When: 특정 파라미터로 호출
        storeQueryService.searchStores(regions, searchName, sortBy);

        // Then: Repository에 동일한 파라미터가 전달됨
        then(storeQueryRepository).should()
                .searchStores(eq(regions), eq(searchName), eq(sortBy));
    }


    // ===== 헬퍼 메서드 =====

    /**
     * Mock Store 생성 헬퍼 메서드
     *
     * 테스트마다 Store 객체를 만드는 코드 중복 제거
     * Location과 Food 객체도 함께 생성하여 N+1 문제 없이 DTO 변환 가능
     */
    private Store createMockStore(Long id, String name, String regionName, FoodName foodName) {
        // Location 엔티티 생성
        Location location = Location.builder()
                .id(1L)
                .name(regionName)
                .build();

        // Food 엔티티 생성
        Food food = Food.builder()
                .id(1L)
                .name(foodName)
                .build();

        // Store 엔티티 생성 (Location, Food 포함)
        return Store.builder()
                .id(id)
                .name(name)
                .managerNumber(1012345678L)
                .detailAddress(regionName + " 테헤란로 123")
                .location(location)
                .food(food)
                .build();
    }
}
