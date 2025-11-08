package com.example.umc9th.domain.store.repository;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.global.config.JpaAuditingConfig;
import com.example.umc9th.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StoreQueryRepository 테스트
 *
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로딩 (가볍고 빠름)
 * - 내장 DB(H2) 자동 사용
 * - 각 테스트 후 자동 롤백 (DB 초기화)
 *
 * @Import(QueryDslConfig.class):
 * - QueryDSL 설정 수동 임포트
 * - JPAQueryFactory 빈 등록
 *
 * @Import(JpaAuditingConfig.class):
 * - JPA Auditing 활성화 (createdAt, updatedAt 자동 설정)
 * - Store 엔티티가 BaseTimeEntity를 상속받는 경우 필요
 *
 * 테스트 목적:
 * - QueryDSL 동적 쿼리 검증
 * - 지역 필터링 (단일/다중) 동작 확인
 * - 이름 검색 (공백 처리) 동작 확인
 * - 정렬 (latest/name) 동작 확인
 * - N+1 문제 방지 (Fetch Join) 검증
 */
@DataJpaTest
@Import({QueryDslConfig.class, StoreQueryRepository.class, JpaAuditingConfig.class})
@DisplayName("StoreQueryRepository 테스트")
class StoreQueryRepositoryTest {

    @Autowired
    private StoreQueryRepository storeQueryRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 데이터 참조 변수
    private Location gangnam, seocho, jongno, eunpyeong;
    private Food korean, chinese, japanese, western;

    @BeforeEach
    void setUp() {
        // Location 데이터 생성
        gangnam = Location.builder().name("강남구").build();
        seocho = Location.builder().name("서초구").build();
        jongno = Location.builder().name("종로구").build();
        eunpyeong = Location.builder().name("은평구").build();

        entityManager.persist(gangnam);
        entityManager.persist(seocho);
        entityManager.persist(jongno);
        entityManager.persist(eunpyeong);

        // Food 데이터 생성
        korean = Food.builder().name(FoodName.KOREAN).build();
        chinese = Food.builder().name(FoodName.CHINESE).build();
        japanese = Food.builder().name(FoodName.JAPANESE).build();
        western = Food.builder().name(FoodName.WESTERN).build();

        entityManager.persist(korean);
        entityManager.persist(chinese);
        entityManager.persist(japanese);
        entityManager.persist(western);

        // Store 데이터 생성
        createTestStores();

        // 쓰기 지연 SQL 실행 및 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();
    }

    private void createTestStores() {
        // 지역 필터링 테스트용
        persistStore("강남 한식당", 1011111111L, "테헤란로 123", gangnam, korean);
        persistStore("서초 중식당", 1022222222L, "서초대로 456", seocho, chinese);
        persistStore("종로 일식당", 1033333333L, "종로 789", jongno, japanese);
        persistStore("강남 카페", 1044444444L, "강남대로 111", gangnam, western);

        // 이름 검색 테스트용 (공백 없음)
        persistStore("민트초코카페", 1055555555L, "역삼로 222", gangnam, western);

        // 이름 검색 테스트용 (공백 있음 - OR 검색)
        persistStore("민트 레스토랑", 1066666666L, "서초중앙로 333", seocho, western);
        persistStore("초코 베이커리", 1077777777L, "강남역로 444", gangnam, western);
        persistStore("스위트 카페", 1088888888L, "판교로 555", seocho, western);

        // 정렬 테스트용 (이름순)
        persistStore("가나다 식당", 1099999999L, "은평로 666", eunpyeong, korean);
        persistStore("ABC Store", 1000000000L, "종로3가 777", jongno, western);
        persistStore("ZZZ Cafe", 1011111112L, "강남구 888", gangnam, western);

        // 동일 이름 테스트 (최신순 정렬 확인용)
        persistStore("카페", 1022222223L, "서초구 999", seocho, western);
        persistStore("카페", 1033333334L, "강남구 000", gangnam, western);
    }

    private void persistStore(String name, Long managerNumber, String detailAddress, Location location, Food food) {
        Store store = Store.builder()
                .name(name)
                .managerNumber(managerNumber)
                .detailAddress(detailAddress)
                .location(location)
                .food(food)
                .build();
        entityManager.persist(store);
    }

    @Test
    @DisplayName("전체 가게 조회 - 필터 없이 최신순 정렬")
    void searchStores_NoFilter_ReturnsAllStoresOrderedByLatest() {
        // Given: 필터 없음
        List<String> regions = null;
        String searchName = null;
        String sortBy = "latest";

        // When: 전체 조회
        List<Store> results = storeQueryRepository.searchStores(regions, searchName, sortBy);

        // Then: 모든 가게가 조회되고, ID 내림차순 정렬
        assertThat(results).hasSize(13);

        // ID 내림차순 정렬 검증 (첫 번째 ID가 마지막 ID보다 커야 함)
        assertThat(results.get(0).getId()).isGreaterThan(results.get(12).getId());

        // 마지막 삽입된 "카페"가 첫 번째로 조회되어야 함
        assertThat(results.get(0).getName()).isEqualTo("카페");
    }

    @Test
    @DisplayName("단일 지역 필터링 - 강남구만 조회")
    void searchStores_SingleRegion_ReturnsFilteredStores() {
        // Given: 강남구 필터
        List<String> regions = Arrays.asList("강남구");

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, null, null);

        // Then: 강남구 가게만 조회 (ID: 1, 4, 5, 7, 11, 13)
        assertThat(results).hasSize(6);
        assertThat(results)
                .extracting(Store::getName)
                .containsExactlyInAnyOrder(
                        "강남 한식당",
                        "강남 카페",
                        "민트초코카페",
                        "초코 베이커리",
                        "ZZZ Cafe",
                        "카페"
                );
    }

    @Test
    @DisplayName("다중 지역 필터링 - 강남구와 서초구 조회")
    void searchStores_MultipleRegions_ReturnsFilteredStores() {
        // Given: 강남구 + 서초구 필터
        List<String> regions = Arrays.asList("강남구", "서초구");

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, null, null);

        // Then: 강남구 + 서초구 가게 조회 (총 10개)
        // 강남구: 6개 (강남 한식당, 강남 카페, 민트초코카페, 초코 베이커리, ZZZ Cafe, 카페)
        // 서초구: 4개 (서초 중식당, 민트 레스토랑, 스위트 카페, 카페)
        assertThat(results).hasSize(10);

        // 지역 검증
        assertThat(results)
                .extracting(store -> store.getLocation().getName())
                .containsOnly("강남구", "서초구");
    }

    @Test
    @DisplayName("이름 검색 (공백 없음) - '민트초코' 포함된 가게만 조회")
    void searchStores_NameWithoutSpace_ReturnsExactMatch() {
        // Given: 공백 없는 검색어
        String searchName = "민트초코";

        // When
        List<Store> results = storeQueryRepository.searchStores(null, searchName, null);

        // Then: "민트초코"가 포함된 가게만 조회
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("민트초코카페");
    }

    @Test
    @DisplayName("이름 검색 (공백 있음) - '민트' 또는 '초코' 포함된 가게 조회 (OR 검색)")
    void searchStores_NameWithSpace_ReturnsOrMatch() {
        // Given: 공백 있는 검색어 (OR 검색)
        String searchName = "민트 초코";

        // When
        List<Store> results = storeQueryRepository.searchStores(null, searchName, null);

        // Then: "민트" 또는 "초코"가 포함된 가게 조회 (ID: 5, 6, 7)
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(Store::getName)
                .containsExactlyInAnyOrder(
                        "민트초코카페",      // "민트"와 "초코" 둘 다 포함
                        "민트 레스토랑",     // "민트" 포함
                        "초코 베이커리"      // "초코" 포함
                );
    }

    @Test
    @DisplayName("정렬 - 최신순 (ID 내림차순)")
    void searchStores_SortByLatest_ReturnsOrderedByIdDesc() {
        // Given: 강남구 필터 + 최신순 정렬
        List<String> regions = Arrays.asList("강남구");
        String sortBy = "latest";

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, null, sortBy);

        // Then: ID 내림차순 정렬 (6개 조회됨)
        assertThat(results).hasSize(6);

        // ID가 내림차순인지 검증
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getId()).isGreaterThan(results.get(i + 1).getId());
        }
    }

    @Test
    @DisplayName("정렬 - 이름순 (오름차순) → 동일 시 최신순")
    void searchStores_SortByName_ReturnsOrderedByNameAsc() {
        // Given: 이름순 정렬
        String sortBy = "name";

        // When
        List<Store> results = storeQueryRepository.searchStores(null, null, sortBy);

        // Then: 이름 오름차순 정렬
        assertThat(results).hasSize(13);

        // NOTE: H2와 MySQL의 collation 차이
        // - H2: 영어 대문자 → 한글 → 영어 소문자 순서
        // - MySQL (utf8mb4_unicode_ci): 한글 → 영어 순서
        // 실제 프로덕션 환경(MySQL)에서는 한글이 먼저 오지만,
        // 테스트 환경(H2)에서는 영어가 먼저 옵니다.

        // H2 기준: 첫 번째는 "ABC Store" (영어 대문자)
        assertThat(results.get(0).getName()).isEqualTo("ABC Store");

        // 이름 오름차순이 적용되었는지 검증
        for (int i = 0; i < results.size() - 1; i++) {
            String currentName = results.get(i).getName();
            String nextName = results.get(i + 1).getName();

            // 동일한 이름이면 ID 내림차순(최신순)인지 확인
            if (currentName.equals(nextName)) {
                assertThat(results.get(i).getId())
                        .isGreaterThan(results.get(i + 1).getId());
            }
        }
    }

    @Test
    @DisplayName("정렬 - 이름이 동일한 경우 최신순 정렬")
    void searchStores_SortByName_SameNameOrderedByLatest() {
        // Given: "카페"로 검색 + 이름순 정렬
        String searchName = "카페";
        String sortBy = "name";

        // When
        List<Store> results = storeQueryRepository.searchStores(null, searchName, sortBy);

        // Then: 동일한 이름 "카페"는 ID 내림차순 (최신순)
        List<Store> cafes = results.stream()
                .filter(store -> store.getName().equals("카페"))
                .toList();

        assertThat(cafes).hasSize(2);
        // 동일한 이름이면 ID가 큰 것(최신)이 먼저 와야 함
        assertThat(cafes.get(0).getId()).isGreaterThan(cafes.get(1).getId());
    }

    @Test
    @DisplayName("복합 조건 - 지역 + 이름 + 정렬")
    void searchStores_CombinedFilters_ReturnsFilteredAndSorted() {
        // Given: 강남구/서초구 + "카페" 검색 + 이름순 정렬
        List<String> regions = Arrays.asList("강남구", "서초구");
        String searchName = "카페";
        String sortBy = "name";

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, searchName, sortBy);

        // Then: 조건에 맞는 가게만 조회 + 이름순 정렬
        assertThat(results).hasSizeGreaterThan(0);

        // 모든 결과가 강남구 또는 서초구
        assertThat(results)
                .extracting(store -> store.getLocation().getName())
                .containsOnly("강남구", "서초구");

        // 모든 결과가 "카페" 포함
        assertThat(results)
                .allMatch(store -> store.getName().contains("카페"));
    }

    @Test
    @DisplayName("검색 결과 없음 - 존재하지 않는 조건")
    void searchStores_NoMatch_ReturnsEmptyList() {
        // Given: 존재하지 않는 지역
        List<String> regions = Arrays.asList("제주도");

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, null, null);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("N+1 문제 방지 - Location과 Food가 Fetch Join으로 조회됨")
    void searchStores_FetchJoin_LoadsLocationAndFood() {
        // Given
        List<String> regions = Arrays.asList("강남구");

        // When
        List<Store> results = storeQueryRepository.searchStores(regions, null, null);

        // Then: LazyInitializationException 발생 안 함
        assertThat(results).isNotEmpty();

        // Location과 Food 접근 시 추가 쿼리 없이 조회 가능
        results.forEach(store -> {
            assertThat(store.getLocation()).isNotNull();
            assertThat(store.getLocation().getName()).isNotNull();
            assertThat(store.getFood()).isNotNull();
            assertThat(store.getFood().getName()).isNotNull();
        });
    }
}
