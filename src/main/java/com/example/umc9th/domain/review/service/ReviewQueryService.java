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
     * 내가 작성한 리뷰 목록 조회 (페이징)
     *
     * @param memberId 회원 ID
     * @param page 페이지 번호 (1부터 시작)
     * @return 페이징된 리뷰 목록
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
