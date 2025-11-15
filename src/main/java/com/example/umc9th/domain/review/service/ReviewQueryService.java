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
     * 회원의 리뷰 조회 (동적 필터링)
     *
     * @param memberId 회원 ID
     * @param storeName 가게 이름 (nullable)
     * @param minScore 최소 별점 (nullable)
     * @param maxScore 최대 별점 (nullable)
     * @return 리뷰 DTO 리스트
     */
    public List<ReviewResponse.MyReview> getMyReviews(Long memberId, String storeName, Float minScore, Float maxScore) {
        log.info("[getMyReviews] memberId={}, storeName={}, minScore={}, maxScore={}",
                memberId, storeName, minScore, maxScore);

        // 별점 범위 검증
        validateScoreRange(minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findMyReviews(memberId, storeName, minScore, maxScore);
        log.info("[getMyReviews] result count: {}", reviews.size());

        return reviews.stream()
                .map(ReviewResponse.MyReview::from)
                .toList();
    }

    public List<ReviewResponse.MyReview> getMyReviewsByStarRange(Long memberId, Float minScore, Float maxScore) {
        log.info("[getMyReviewsByStarRange] memberId={}, range={}-{}", memberId, minScore, maxScore);

        // 별점 범위 검증
        validateScoreRange(minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findMyReviewsByStarRange(memberId, minScore, maxScore);

        return reviews.stream()
                .map(ReviewResponse.MyReview::from)
                .toList();
    }

    public List<ReviewResponse.MyReview> getMyReviewsByStore(Long memberId, Long storeId) {
        log.info("[getMyReviewsByStore] memberId={}, storeId={}", memberId, storeId);

        List<Review> reviews = reviewQueryRepository.findMyReviewsByStore(memberId, storeId);

        return reviews.stream()
                .map(ReviewResponse.MyReview::from)
                .toList();
    }

    /**
     * 범용 리뷰 조회 (RESTful API 통합 메서드)
     *
     * @param memberId 회원 ID (nullable)
     * @param storeId 가게 ID (nullable)
     * @param storeName 가게 이름 (nullable)
     * @param minScore 최소 별점 (nullable)
     * @param maxScore 최대 별점 (nullable)
     * @return 리뷰 DTO 리스트
     */
    public List<ReviewResponse.MyReview> getReviews(Long memberId, Long storeId, String storeName, Float minScore, Float maxScore) {
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
