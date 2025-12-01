package com.example.umc9th.domain.review.service;

import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.entity.Review;
import com.example.umc9th.domain.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewQueryRepository reviewQueryRepository;

    /**
     * 내가 작성한 리뷰 목록 조회 (페이징 - Page 방식)
     *
     * COUNT 쿼리를 실행하여 전체 페이지 수와 전체 데이터 개수를 제공합니다.
     * 페이지 번호 UI (1, 2, 3...)가 필요한 경우 사용하세요.
     *
     * @param memberId 회원 ID
     * @param page 페이지 번호 (1부터 시작)
     * @return 페이징된 리뷰 목록 (전체 개수 포함)
     */
    public ReviewResponse.ReviewPreViewListDTO getMyReviewsList(Long memberId, Integer page) {
        log.info("[getMyReviewsList] memberId={}, page={}", memberId, page);

        // PageRequest 생성 (0-based index로 변환)
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest
                .of(page - 1, 10);

        // QueryRepository의 페이징 메서드 사용 (일관성 확보)
        org.springframework.data.domain.Page<Review> reviewPage = reviewQueryRepository.findReviewsWithPaging(
                memberId, null, null, null, null, pageRequest);

        return com.example.umc9th.domain.review.converter.ReviewConverter.toReviewPreViewListDTO(reviewPage);
    }

    /**
     * 내가 작성한 리뷰 목록 조회 (무한 스크롤 - Slice 방식)
     *
     * COUNT 쿼리를 실행하지 않아 성능이 우수합니다.
     * 무한 스크롤 UI에 적합한 방식입니다.
     *
     * @param memberId 회원 ID
     * @param page 페이지 번호 (1부터 시작)
     * @return 무한 스크롤용 리뷰 목록 (다음 페이지 존재 여부만 포함)
     */
    public ReviewResponse.ReviewPreViewSliceDTO getMyReviewsListWithSlice(Long memberId, Integer page) {
        log.info("[getMyReviewsListWithSlice] memberId={}, page={}", memberId, page);

        // PageRequest 생성 (0-based index로 변환)
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest
                .of(page - 1, 10);

        // QueryRepository의 Slice 메서드 사용 (COUNT 쿼리 없음!)
        org.springframework.data.domain.Slice<Review> reviewSlice = reviewQueryRepository.findReviewsWithSlice(
                memberId, null, null, null, null, pageRequest);

        return com.example.umc9th.domain.review.converter.ReviewConverter.toReviewPreViewSliceDTO(reviewSlice);
    }

    /**
     * 범용 리뷰 조회 (RESTful API 통합 메서드)
     * 모든 필터 조건을 선택적으로 지원합니다.
     *
     * @param memberId  회원 ID (nullable)
     * @param storeId   가게 ID (nullable)
     * @param storeName 가게 이름 (nullable)
     * @param minScore  최소 별점 (nullable)
     * @param maxScore  최대 별점 (nullable)
     * @return 리뷰 DTO 리스트
     */
    public List<ReviewResponse.MyReview> getReviews(Long memberId, Long storeId, String storeName, Float minScore,
            Float maxScore) {
        log.info("[getReviews] memberId={}, storeId={}, storeName={}, minScore={}, maxScore={}",
                memberId, storeId, storeName, minScore, maxScore);

        // 별점 범위 검증
        validateScoreRange(minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findReviews(memberId, storeId, storeName, minScore, maxScore);
        log.info("[getReviews] result count: {}", reviews.size());

        return reviews.stream()
                .map(ReviewResponse.MyReview::from)
                .toList();
    }

    /**
     * 별점 범위 검증
     * - 별점은 0.0 ~ 5.0 범위여야 함
     * - minScore가 maxScore보다 클 수 없음
     *
     * @param minScore 최소 별점 (nullable)
     * @param maxScore 최대 별점 (nullable)
     * @throws IllegalArgumentException 유효하지 않은 범위일 경우
     */
    private void validateScoreRange(Float minScore, Float maxScore) {
        if (minScore != null) {
            if (minScore < 0.0f || minScore > 5.0f) {
                log.warn("[validateScoreRange] 잘못된 최소 별점: {}", minScore);
                throw new IllegalArgumentException("최소 별점은 0.0 이상 5.0 이하여야 합니다");
            }
        }

        if (maxScore != null) {
            if (maxScore < 0.0f || maxScore > 5.0f) {
                log.warn("[validateScoreRange] 잘못된 최대 별점: {}", maxScore);
                throw new IllegalArgumentException("최대 별점은 0.0 이상 5.0 이하여야 합니다");
            }
        }

        if (minScore != null && maxScore != null) {
            if (minScore > maxScore) {
                log.warn("[validateScoreRange] 최소 별점이 최대 별점보다 큼: min={}, max={}", minScore, maxScore);
                throw new IllegalArgumentException("최소 별점은 최대 별점보다 클 수 없습니다");
            }
        }
    }
}
