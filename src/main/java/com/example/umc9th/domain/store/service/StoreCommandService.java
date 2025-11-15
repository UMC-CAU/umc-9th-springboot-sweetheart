package com.example.umc9th.domain.store.service;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.location.repository.LocationRepository;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.repository.FoodRepository;
import com.example.umc9th.domain.store.dto.StoreRequest;
import com.example.umc9th.domain.store.dto.StoreResponse;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가게 Command Service (CUD 작업)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoreCommandService {

    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final FoodRepository foodRepository;

    /**
     * 특정 지역에 가게 추가하기
     * @param request 가게 추가 요청 DTO
     * @return 생성된 가게 정보
     */
    public StoreResponse.CreateStore createStore(StoreRequest.CreateStoreDTO request) {
        log.info("[StoreCommandService.createStore] name: {}, locationId: {}, foodId: {}",
                request.getName(), request.getLocationId(), request.getFoodId());

        // 지역 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOCATION_NOT_FOUND));

        // 음식 카테고리 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 가게 엔티티 생성 및 저장
        Store store = Store.builder()
                .name(request.getName())
                .managerNumber(request.getManagerNumber())
                .detailAddress(request.getDetailAddress())
                .location(location)
                .food(food)
                .build();

        Store savedStore = storeRepository.save(store);

        log.info("[StoreCommandService.createStore] 가게 추가 완료 - storeId: {}", savedStore.getId());

        return StoreResponse.CreateStore.from(savedStore);
    }
}
