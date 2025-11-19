package com.example.umc9th.domain.review.service;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.domain.review.dto.ReviewRequest;
import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.entity.Review;
import com.example.umc9th.domain.review.repository.ReviewRepository;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리뷰 Command Service (CUD 작업)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    /**
     * 리뷰 작성
     * @param request 리뷰 작성 요청 DTO
     * @return 생성된 리뷰 정보
     */
    public ReviewResponse.CreateReview createReview(ReviewRequest.CreateReviewDTO request) {
        log.info("[ReviewCommandService.createReview] storeId: {}, memberId: {}, star: {}",
                request.getStoreId(), request.getMemberId(), request.getStar());

        // 회원 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 가게 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 리뷰 엔티티 생성 및 저장
        Review review = Review.builder()
                .content(request.getContent())
                .star(request.getStar())
                .store(store)
                .user(member)
                .build();

        Review savedReview = reviewRepository.save(review);

        log.info("[ReviewCommandService.createReview] 리뷰 작성 완료 - reviewId: {}", savedReview.getId());

        return ReviewResponse.CreateReview.from(savedReview);
    }
}
