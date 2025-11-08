package com.example.umc9th.domain.review.repository;

import com.example.umc9th.domain.review.entity.Review;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// Q클래스 정의 대신 import static 사용
import static com.example.umc9th.domain.review.entity.QReview.review;
import static com.example.umc9th.domain.store.entity.QStore.store;
import static com.example.umc9th.domain.member.entity.QMember.member;

/**
 * QueryDSL을 사용한 Review 동적 쿼리 Repository
 */
@Repository
@RequiredArgsConstructor
public class ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 회원의 리뷰를 동적 조건으로 조회
     *
     * @param memberId 회원 ID
     * @param storeName 가게 이름 (부분 일치, nullable)
     * @param minScore 최소 별점 (nullable)
     * @param maxScore 최대 별점 (nullable)
     * @return 조건에 맞는 리뷰 리스트 (Fetch Join 적용)
     */
    public List<Review> findMyReviews(Long memberId, String storeName, Float minScore, Float maxScore) {
        return queryFactory
                .selectFrom(review)
                .distinct()
                .join(review.store, store).fetchJoin()
                .join(review.user, member).fetchJoin()
                .where(
                    review.user.id.eq(memberId),
                    storeNameContains(storeName),
                    starBetween(minScore, maxScore)
                )
                .orderBy(review.createdAt.desc())
                .fetch();
    }

    /**
     * 별점 범위로 회원의 리뷰 조회
     */
    public List<Review> findMyReviewsByStarRange(Long memberId, Float minScore, Float maxScore) {
        return queryFactory
                .selectFrom(review)
                .distinct()
                .join(review.store, store).fetchJoin()
                .join(review.user, member).fetchJoin()
                .where(
                    review.user.id.eq(memberId),
                    starBetween(minScore, maxScore)
                )
                .orderBy(review.createdAt.desc())
                .fetch();
    }

    /**
     * 특정 가게에 대한 회원의 리뷰 조회
     */
    public List<Review> findMyReviewsByStore(Long memberId, Long storeId) {
        return queryFactory
                .selectFrom(review)
                .distinct()
                .join(review.store, store).fetchJoin()
                .where(
                    review.user.id.eq(memberId),
                    review.store.id.eq(storeId)
                )
                .orderBy(review.createdAt.desc())
                .fetch();
    }

    private BooleanExpression storeNameContains(String storeName) {
        return storeName != null ? store.name.contains(storeName) : null;
    }

    /**
     * 별점 범위 조건 생성 (null 처리로 동적 쿼리 구현)
     * - 둘 다 있으면: BETWEEN
     * - minScore만: >=
     * - maxScore만: <=
     * - 둘 다 없으면: 조건 무시
     */
    private BooleanExpression starBetween(Float minScore, Float maxScore) {
        if (minScore != null && maxScore != null) {
            return review.star.between(minScore, maxScore);
        } else if (minScore != null) {
            return review.star.goe(minScore);
        } else if (maxScore != null) {
            return review.star.loe(maxScore);
        }
        return null;
    }

    /**
     * 범용 리뷰 검색 (모든 필터 조건 지원)
     *
     * RESTful API 설계를 위한 통합 메서드
     * - 회원 중심: memberId만 지정
     * - 가게 중심: storeId만 지정
     * - 전체 검색: 모든 조건 조합 가능
     *
     * @param memberId 회원 ID (nullable)
     * @param storeId 가게 ID (nullable)
     * @param storeName 가게 이름 부분 일치 (nullable)
     * @param minScore 최소 별점 (nullable)
     * @param maxScore 최대 별점 (nullable)
     * @return 조건에 맞는 리뷰 리스트 (Fetch Join 적용)
     */
    public List<Review> findReviews(Long memberId, Long storeId, String storeName, Float minScore, Float maxScore) {
        return queryFactory
                .selectFrom(review)
                .distinct()
                .join(review.store, store).fetchJoin()
                .join(review.user, member).fetchJoin()
                .where(
                    memberIdEq(memberId),
                    storeIdEq(storeId),
                    storeNameContains(storeName),
                    starBetween(minScore, maxScore)
                )
                .orderBy(review.createdAt.desc())
                .fetch();
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? review.user.id.eq(memberId) : null;
    }

    private BooleanExpression storeIdEq(Long storeId) {
        return storeId != null ? review.store.id.eq(storeId) : null;
    }
}
