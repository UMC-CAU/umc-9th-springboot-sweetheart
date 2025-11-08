package com.example.umc9th.domain.store.service;

import com.example.umc9th.domain.store.dto.StoreResponse;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Store 조회 전용 Service
 * QueryDSL을 활용한 복잡한 검색 기능을 제공합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreQueryService {

    private final StoreQueryRepository storeQueryRepository;

    /**
     * 가게 검색 - 지역 필터링 + 이름 검색 + 정렬 (다중 선택 지원)
     *
     * @param regions 지역 이름 리스트 (null이면 전체 조회)
     * @param searchName 검색할 가게 이름 (공백 처리 로직 적용)
     * @param sortBy 정렬 기준 ("latest" 또는 "name", null이면 기본값 "latest")
     * @return 검색 결과 DTO 리스트
     */
    public List<StoreResponse.SearchResult> searchStores(List<String> regions, String searchName, String sortBy) {
        log.info("[StoreQueryService.searchStores] regions={}, searchName={}, sortBy={}",
                regions, searchName, sortBy);

        List<Store> stores = storeQueryRepository.searchStores(regions, searchName, sortBy);
        log.info("[StoreQueryService.searchStores] result count: {}", stores.size());

        return stores.stream()
                .map(StoreResponse.SearchResult::from)
                .toList();
    }

    /**
     * 특정 지역의 가게 개수 조회
     *
     * @param regionName 지역 이름
     * @return 가게 수
     */
    public Long countStoresByRegion(String regionName) {
        log.info("[StoreQueryService.countStoresByRegion] regionName={}", regionName);

        Long count = storeQueryRepository.countStoresByRegion(regionName);
        log.info("[StoreQueryService.countStoresByRegion] count: {}", count);

        return count;
    }

    /**
     * 특정 음식 카테고리의 가게 조회
     *
     * @param foodId 음식 카테고리 ID
     * @return 가게 리스트
     */
    public List<StoreResponse.SearchResult> getStoresByFood(Long foodId) {
        log.info("[StoreQueryService.getStoresByFood] foodId={}", foodId);

        List<Store> stores = storeQueryRepository.findStoresByFoodId(foodId);

        return stores.stream()
                .map(StoreResponse.SearchResult::from)
                .toList();
    }
}
