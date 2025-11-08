package com.example.umc9th.domain.store.repository;

import com.example.umc9th.domain.store.entity.Store;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

import static com.example.umc9th.domain.store.entity.QStore.store;
import static com.example.umc9th.domain.location.entity.QLocation.location;
import static com.example.umc9th.domain.member.entity.QFood.food;

/**
 * QueryDSL을 사용한 Store 동적 쿼리 Repository
 * 복잡한 검색 조건, N+1 문제 해결, 동적 필터링 등을 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class StoreQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 가게 검색 - 지역 필터링 + 이름 검색 + 정렬 (다중 선택 지원)
     *
     * @param regions 지역 이름 리스트 (null이면 조건 무시, 다중 선택 가능)
     * @param searchName 검색할 가게 이름 (공백 처리 로직 적용)
     * @param sortBy 정렬 기준 ("latest" 또는 "name", null이면 기본값 "latest")
     * @return 검색 조건에 맞는 가게 리스트 (Location, Food 정보 포함)
     */
    public List<Store> searchStores(List<String> regions, String searchName, String sortBy) {
        return queryFactory
                .selectFrom(store)
                .distinct()
                // N+1 문제 방지: Fetch Join으로 한 번에 조회
                .leftJoin(store.location, location).fetchJoin()
                .leftJoin(store.food, food).fetchJoin()
                .where(
                        regionIn(regions),
                        nameSearch(searchName)
                )
                .orderBy(getSortOrder(sortBy))
                .fetch();
    }

    /**
     * 특정 지역의 가게 개수 조회
     *
     * @param regionName 지역 이름
     * @return 해당 지역의 가게 수
     */
    public Long countStoresByRegion(String regionName) {
        return queryFactory
                .select(store.count())
                .from(store)
                .join(store.location, location)
                .where(location.name.eq(regionName))
                .fetchOne();
    }

    /**
     * 특정 음식 카테고리의 가게 조회
     *
     * @param foodId 음식 카테고리 ID
     * @return 해당 카테고리의 가게 리스트
     */
    public List<Store> findStoresByFoodId(Long foodId) {
        return queryFactory
                .selectFrom(store)
                .distinct()
                .leftJoin(store.location, location).fetchJoin()
                .leftJoin(store.food, food).fetchJoin()
                .where(store.food.id.eq(foodId))
                .fetch();
    }

    // ========== 동적 조건 메서드 (BooleanExpression) ==========

    /**
     * 지역 필터링 조건 (다중 선택 지원)
     * - regions가 null이거나 비어있으면 조건 무시
     * - 여러 지역을 선택한 경우 OR 조건으로 처리 (IN 절 사용)
     *
     * 예: regions = ["강남구", "서초구"]
     *     → location.name IN ('강남구', '서초구')
     */
    private BooleanExpression regionIn(List<String> regions) {
        if (regions == null || regions.isEmpty()) {
            return null;
        }
        return location.name.in(regions);
    }

    // ========== 동적 정렬 메서드 (OrderSpecifier) ==========

    /**
     * 정렬 조건 생성 (동적 정렬)
     * - sortBy가 null이거나 "latest"인 경우: 최신순 (ID 내림차순)
     * - sortBy가 "name"인 경우: 이름순 (오름차순) → 동일 시 최신순
     *
     * 정렬 우선순위 (미션 요구사항):
     * - 가나다 → 영어 대문자 → 영어 소문자 → 특수문자
     * - 실무 구현: DB의 기본 collation을 따름 (MySQL의 경우 utf8mb4_unicode_ci)
     *
     * @param sortBy 정렬 기준 ("latest" 또는 "name")
     * @return 정렬 조건 배열
     */
    private OrderSpecifier<?>[] getSortOrder(String sortBy) {
        // name으로 정렬: 이름 오름차순 → 동일 시 최신순
        if ("name".equalsIgnoreCase(sortBy)) {
            return new OrderSpecifier<?>[]{
                    store.name.asc(),     // 1순위: 이름 오름차순
                    store.id.desc()       // 2순위: 최신순 (동일 이름 처리)
            };
        }

        // latest 또는 기타: 최신순 (기본값)
        return new OrderSpecifier<?>[]{
                store.id.desc()
        };
    }

    /**
     * 이름 검색 조건 (공백 처리 로직)
     * - searchName이 null이거나 비어있으면 조건 무시
     * - 공백 있음: 각 단어를 OR 조건으로 검색 (합집합)
     * - 공백 없음: 전체 문자열로 검색
     *
     * 예시:
     * 1) searchName = "민트 초코"
     *    → name LIKE '%민트%' OR name LIKE '%초코%'
     *    → "민트카페", "초코케이크", "민트초코" 모두 검색됨
     *
     * 2) searchName = "민트초코"
     *    → name LIKE '%민트초코%'
     *    → "민트초코" 포함된 가게만 검색됨
     */
    private BooleanExpression nameSearch(String searchName) {
        // null이거나 빈 문자열이면 조건 무시
        if (searchName == null || searchName.trim().isEmpty()) {
            return null;
        }

        // 앞뒤 공백 제거 후 공백으로 분리
        String[] keywords = searchName.trim().split("\\s+");

        // 공백이 없는 경우 (키워드가 1개): 단일 검색
        if (keywords.length == 1) {
            return store.name.contains(keywords[0]);
        }

        // 공백이 있는 경우 (키워드가 여러 개): OR 검색 (합집합)
        BooleanExpression[] expressions = Arrays.stream(keywords)
                .map(keyword -> store.name.contains(keyword))
                .toArray(BooleanExpression[]::new);

        // anyOf: OR 조건으로 연결
        return Expressions.anyOf(expressions);
    }
}
