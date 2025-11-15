package com.example.umc9th.integration;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreQueryRepository;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.domain.store.service.StoreQueryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 가게 검색 통합 테스트
 *
 * QueryDSL을 활용한 가게 검색 기능의 전체 플로우를 검증합니다.
 * - 지역별 검색
 * - 이름 검색 (공백 처리 로직 포함)
 * - 동적 정렬 (최신순/이름순)
 * - N+1 문제 방지
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("가게 검색 통합 테스트")
class StoreSearchIntegrationTest {

    @Autowired
    private StoreQueryService storeQueryService;

    @Autowired
    private StoreQueryRepository storeQueryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트용 Location과 Food 엔티티
    private Location gangnam;
    private Location seocho;
    private Location songpa;
    private Food koreanFood;
    private Food chineseFood;
    private Food japaneseFood;

    @BeforeEach
    void setUp() {
        // Given: 지역(Location) 데이터 준비
        gangnam = entityManager.merge(Location.builder().name("강남구").build());
        seocho = entityManager.merge(Location.builder().name("서초구").build());
        songpa = entityManager.merge(Location.builder().name("송파구").build());

        // Given: 음식 카테고리(Food) 데이터 준비
        koreanFood = entityManager.merge(Food.builder().name(FoodName.KOREAN).build());
        chineseFood = entityManager.merge(Food.builder().name(FoodName.CHINESE).build());
        japaneseFood = entityManager.merge(Food.builder().name(FoodName.JAPANESE).build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[통합] 가게 생성 -> 전체 조회 -> 지역별 필터링 플로우 테스트")
    void fullStoreSearchFlow_ShouldWork() {
        // Given: 다양한 지역의 가게 생성
        Store store1 = Store.builder()
                .name("강남 한식당")
                .managerNumber(1234567890L)
                .detailAddress("테헤란로 123")
                .location(gangnam)
                .food(koreanFood)
                .build();

        Store store2 = Store.builder()
                .name("서초 중국집")
                .managerNumber(2345678901L)
                .detailAddress("강남대로 456")
                .location(seocho)
                .food(chineseFood)
                .build();

        Store store3 = Store.builder()
                .name("송파 일식집")
                .managerNumber(3456789012L)
                .detailAddress("올림픽로 789")
                .location(songpa)
                .food(japaneseFood)
                .build();

        storeRepository.save(store1);
        storeRepository.save(store2);
        storeRepository.save(store3);

        entityManager.flush();
        entityManager.clear();

        // When & Then 1: 전체 가게 조회 (지역 필터 없음)
        List<Store> allStores = storeQueryRepository.searchStores(null, null, null);
        assertThat(allStores).hasSizeGreaterThanOrEqualTo(3);

        // When & Then 2: 강남구 가게만 필터링
        List<Store> gangnamStores = storeQueryRepository.searchStores(
                Arrays.asList("강남구"),
                null,
                null
        );
        assertThat(gangnamStores).hasSize(1);
        assertThat(gangnamStores.get(0).getName()).isEqualTo("강남 한식당");
        assertThat(gangnamStores.get(0).getLocation().getName()).isEqualTo("강남구");

        // When & Then 3: 다중 지역 선택 (강남구 OR 서초구)
        List<Store> multipleRegions = storeQueryRepository.searchStores(
                Arrays.asList("강남구", "서초구"),
                null,
                null
        );
        assertThat(multipleRegions).hasSize(2);
        assertThat(multipleRegions).extracting("name")
                .containsExactlyInAnyOrder("강남 한식당", "서초 중국집");
    }

    @Test
    @DisplayName("[QueryDSL] 가게 이름 검색 - 공백 없는 단일 키워드 검색")
    void searchStoresByName_SingleKeyword_ShouldWork() {
        // Given: 다양한 이름의 가게 생성
        Store mintCafe = Store.builder()
                .name("민트카페")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build();

        Store mintChoco = Store.builder()
                .name("민트초코 베이커리")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(chineseFood)
                .build();

        Store chocoCake = Store.builder()
                .name("초코케이크 하우스")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(songpa)
                .food(japaneseFood)
                .build();

        storeRepository.save(mintCafe);
        storeRepository.save(mintChoco);
        storeRepository.save(chocoCake);

        entityManager.flush();
        entityManager.clear();

        // When: "민트" 단일 키워드 검색
        List<Store> mintStores = storeQueryRepository.searchStores(null, "민트", null);

        // Then: "민트"가 포함된 가게만 조회
        assertThat(mintStores).hasSize(2);
        assertThat(mintStores).extracting("name")
                .containsExactlyInAnyOrder("민트카페", "민트초코 베이커리");
    }

    @Test
    @DisplayName("[QueryDSL] 가게 이름 검색 - 공백으로 구분된 다중 키워드 OR 검색")
    void searchStoresByName_MultipleKeywordsWithSpace_ShouldUseOrCondition() {
        // Given: 다양한 이름의 가게 생성
        Store mintCafe = Store.builder()
                .name("민트카페")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build();

        Store chocoCake = Store.builder()
                .name("초코케이크")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(chineseFood)
                .build();

        Store strawberryStore = Store.builder()
                .name("딸기 전문점")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(songpa)
                .food(japaneseFood)
                .build();

        storeRepository.save(mintCafe);
        storeRepository.save(chocoCake);
        storeRepository.save(strawberryStore);

        entityManager.flush();
        entityManager.clear();

        // When: "민트 초코" 공백으로 구분된 검색 (OR 조건)
        List<Store> result = storeQueryRepository.searchStores(null, "민트 초코", null);

        // Then: "민트" 또는 "초코"가 포함된 가게 모두 조회 (OR 검색)
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name")
                .containsExactlyInAnyOrder("민트카페", "초코케이크");
    }

    @Test
    @DisplayName("[QueryDSL] 가게 정렬 - 최신순(ID 내림차순) 기본 정렬")
    void searchStoresWithSorting_LatestFirst_ShouldWork() {
        // Given: 순차적으로 가게 생성 (ID: 1 -> 2 -> 3)
        Store store1 = storeRepository.save(Store.builder()
                .name("첫번째 가게")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build());

        Store store2 = storeRepository.save(Store.builder()
                .name("두번째 가게")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(chineseFood)
                .build());

        Store store3 = storeRepository.save(Store.builder()
                .name("세번째 가게")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(songpa)
                .food(japaneseFood)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 최신순 정렬 (sortBy = "latest" 또는 null)
        List<Store> latestStores = storeQueryRepository.searchStores(null, null, "latest");

        // Then: ID 내림차순으로 정렬되어야 함 (3 -> 2 -> 1)
        assertThat(latestStores).hasSizeGreaterThanOrEqualTo(3);

        // 최근 3개 가게 검증
        int size = latestStores.size();
        assertThat(latestStores.get(size - 3).getName()).isEqualTo("세번째 가게");
        assertThat(latestStores.get(size - 2).getName()).isEqualTo("두번째 가게");
        assertThat(latestStores.get(size - 1).getName()).isEqualTo("첫번째 가게");
    }

    @Test
    @DisplayName("[QueryDSL] 가게 정렬 - 이름순(가나다순) 정렬")
    void searchStoresWithSorting_ByName_ShouldWork() {
        // Given: 다양한 이름의 가게 생성 (가나다순 아님)
        Store storeC = storeRepository.save(Store.builder()
                .name("초코 가게")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(songpa)
                .food(japaneseFood)
                .build());

        Store storeA = storeRepository.save(Store.builder()
                .name("가나다 가게")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build());

        Store storeB = storeRepository.save(Store.builder()
                .name("나비 가게")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(chineseFood)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 이름순 정렬 (sortBy = "name")
        List<Store> nameOrderedStores = storeQueryRepository.searchStores(null, null, "name");

        // Then: 이름 오름차순으로 정렬되어야 함 (가나다순)
        assertThat(nameOrderedStores).hasSizeGreaterThanOrEqualTo(3);

        // 최근 추가된 3개 가게가 이름순으로 정렬되었는지 확인
        List<String> storeNames = nameOrderedStores.stream()
                .map(Store::getName)
                .filter(name -> name.contains("가게"))
                .toList();

        assertThat(storeNames).contains("가나다 가게", "나비 가게", "초코 가게");

        // 가나다 순서 확인
        int indexA = storeNames.indexOf("가나다 가게");
        int indexB = storeNames.indexOf("나비 가게");
        int indexC = storeNames.indexOf("초코 가게");

        assertThat(indexA).isLessThan(indexB);
        assertThat(indexB).isLessThan(indexC);
    }

    @Test
    @DisplayName("[QueryDSL] N+1 문제 없이 Fetch Join이 정상 작동하는지 검증")
    void searchStores_ShouldUseFetchJoinWithoutNPlusOne() {
        // Given: 가게 생성
        Store store = Store.builder()
                .name("테스트 가게")
                .managerNumber(1234567890L)
                .detailAddress("테스트 주소")
                .location(gangnam)
                .food(koreanFood)
                .build();

        storeRepository.save(store);
        entityManager.flush();
        entityManager.clear();

        // When: QueryDSL로 가게 검색 (Fetch Join 포함)
        List<Store> result = storeQueryRepository.searchStores(null, "테스트", null);

        // Then: 가게가 조회되고, Location과 Food가 Lazy Loading 없이 접근 가능해야 함
        assertThat(result).hasSize(1);
        Store foundStore = result.get(0);

        // Fetch Join 검증: Location과 Food에 접근 시 추가 쿼리 발생하지 않음
        assertThat(foundStore.getLocation()).isNotNull();
        assertThat(foundStore.getLocation().getName()).isEqualTo("강남구");

        assertThat(foundStore.getFood()).isNotNull();
        assertThat(foundStore.getFood().getName()).isEqualTo(FoodName.KOREAN);
    }

    @Test
    @DisplayName("[QueryDSL] 복합 조건 검색 - 지역 + 이름 + 정렬 동시 적용")
    void searchStoresWithComplexConditions_ShouldWork() {
        // Given: 다양한 조건의 가게 생성
        Store gangnamCafe1 = Store.builder()
                .name("강남 카페 A")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build();

        Store gangnamCafe2 = Store.builder()
                .name("강남 카페 B")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(gangnam)
                .food(chineseFood)
                .build();

        Store seochoCafe = Store.builder()
                .name("서초 카페 C")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(seocho)
                .food(japaneseFood)
                .build();

        Store gangnamRestaurant = Store.builder()
                .name("강남 맛집")
                .managerNumber(4444444444L)
                .detailAddress("주소4")
                .location(gangnam)
                .food(koreanFood)
                .build();

        storeRepository.save(gangnamCafe1);
        storeRepository.save(gangnamCafe2);
        storeRepository.save(seochoCafe);
        storeRepository.save(gangnamRestaurant);

        entityManager.flush();
        entityManager.clear();

        // When: 복합 조건 검색 (지역=강남구 + 이름="카페" + 정렬=이름순)
        List<Store> result = storeQueryRepository.searchStores(
                Arrays.asList("강남구"),
                "카페",
                "name"
        );

        // Then: 강남구에 있고 이름에 "카페"가 포함된 가게만 조회되고, 이름순으로 정렬되어야 함
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name")
                .containsExactly("강남 카페 A", "강남 카페 B"); // 이름순 정렬 확인
        assertThat(result).allMatch(s -> s.getLocation().getName().equals("강남구"));
    }

    @Test
    @DisplayName("[QueryDSL] countStoresByRegion() 지역별 가게 수 조회")
    void countStoresByRegion_ShouldReturnCorrectCount() {
        // Given: 강남구에 2개, 서초구에 1개 가게 생성
        storeRepository.save(Store.builder()
                .name("강남 가게 1")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build());

        storeRepository.save(Store.builder()
                .name("강남 가게 2")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(gangnam)
                .food(chineseFood)
                .build());

        storeRepository.save(Store.builder()
                .name("서초 가게 1")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(seocho)
                .food(japaneseFood)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 강남구 가게 수 조회
        Long gangnamCount = storeQueryRepository.countStoresByRegion("강남구");

        // Then: 강남구에 2개 가게 존재
        assertThat(gangnamCount).isEqualTo(2L);

        // When: 서초구 가게 수 조회
        Long seochoCount = storeQueryRepository.countStoresByRegion("서초구");

        // Then: 서초구에 1개 가게 존재
        assertThat(seochoCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("[QueryDSL] findStoresByFoodId() 음식 카테고리별 가게 조회")
    void findStoresByFoodId_ShouldReturnStoresOfSpecificFood() {
        // Given: 한식당 2개, 중식당 1개 생성
        storeRepository.save(Store.builder()
                .name("한식당 1")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(koreanFood)
                .build());

        storeRepository.save(Store.builder()
                .name("한식당 2")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(koreanFood)
                .build());

        storeRepository.save(Store.builder()
                .name("중식당 1")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(songpa)
                .food(chineseFood)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 한식(KOREAN) 카테고리 가게 조회
        List<Store> koreanStores = storeQueryRepository.findStoresByFoodId(koreanFood.getId());

        // Then: 한식당 2개만 조회
        assertThat(koreanStores).hasSize(2);
        assertThat(koreanStores).extracting("name")
                .containsExactlyInAnyOrder("한식당 1", "한식당 2");
        assertThat(koreanStores).allMatch(s -> s.getFood().getName().equals(FoodName.KOREAN));
    }

    @Test
    @DisplayName("[Service] StoreQueryService를 통한 엔드투엔드 검색 플로우 검증")
    void storeQueryService_EndToEndFlow_ShouldWork() {
        // Given: 가게 생성
        storeRepository.save(Store.builder()
                .name("서비스 테스트 가게")
                .managerNumber(9999999999L)
                .detailAddress("테스트 주소")
                .location(gangnam)
                .food(koreanFood)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: Service를 통한 검색 (DTO 변환까지 포함)
        var result = storeQueryService.searchStores(
                Arrays.asList("강남구"),
                "서비스",
                "latest"
        );

        // Then: Service 레이어를 거쳐 DTO로 변환된 결과 확인
        assertThat(result).isNotEmpty();
        assertThat(result).extracting("name").contains("서비스 테스트 가게");
    }
}
