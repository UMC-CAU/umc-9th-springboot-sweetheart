package com.example.umc9th.domain.review.service;

import com.example.umc9th.domain.review.dto.ReviewDTO;
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
    public List<ReviewDTO.MyReview> getMyReviews(Long memberId, String storeName, Float minScore, Float maxScore) {
        log.info("[getMyReviews] memberId={}, storeName={}, minScore={}, maxScore={}",
                memberId, storeName, minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findMyReviews(memberId, storeName, minScore, maxScore);
        log.info("[getMyReviews] result count: {}", reviews.size());

        return reviews.stream()
                .map(ReviewDTO.MyReview::from)
                .toList();
    }

    public List<ReviewDTO.MyReview> getMyReviewsByStarRange(Long memberId, Float minScore, Float maxScore) {
        log.info("[getMyReviewsByStarRange] memberId={}, range={}-{}", memberId, minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findMyReviewsByStarRange(memberId, minScore, maxScore);

        return reviews.stream()
                .map(ReviewDTO.MyReview::from)
                .toList();
    }

    public List<ReviewDTO.MyReview> getMyReviewsByStore(Long memberId, Long storeId) {
        log.info("[getMyReviewsByStore] memberId={}, storeId={}", memberId, storeId);

        List<Review> reviews = reviewQueryRepository.findMyReviewsByStore(memberId, storeId);

        return reviews.stream()
                .map(ReviewDTO.MyReview::from)
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
    public List<ReviewDTO.MyReview> getReviews(Long memberId, Long storeId, String storeName, Float minScore, Float maxScore) {
        log.info("[getReviews] memberId={}, storeId={}, storeName={}, minScore={}, maxScore={}",
                memberId, storeId, storeName, minScore, maxScore);

        List<Review> reviews = reviewQueryRepository.findReviews(memberId, storeId, storeName, minScore, maxScore);
        log.info("[getReviews] result count: {}", reviews.size());

        return reviews.stream()
                .map(ReviewDTO.MyReview::from)
                .toList();
    }
}
