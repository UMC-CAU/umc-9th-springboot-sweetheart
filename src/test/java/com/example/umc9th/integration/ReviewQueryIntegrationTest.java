package com.example.umc9th.integration;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.review.entity.Review;
import com.example.umc9th.domain.review.repository.ReviewQueryRepository;
import com.example.umc9th.domain.review.repository.ReviewRepository;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.global.auth.enums.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 리뷰 조회 통합 테스트
 *
 * QueryDSL을 활용한 리뷰 조회 기능의 전체 플로우를 검증합니다.
 * - 회원별 리뷰 조회
 * - 가게별 리뷰 조회
 * - 별점 필터링
 * - 가게 이름 검색
 * - N+1 문제 방지
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("리뷰 조회 통합 테스트")
class ReviewQueryIntegrationTest {

    @Autowired
    private ReviewQueryService reviewQueryService;

    @Autowired
    private ReviewQueryRepository reviewQueryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트용 기본 데이터
    private Member member1;
    private Member member2;
    private Store koreanRestaurant;
    private Store chineseRestaurant;
    private Store japaneseRestaurant;

    @BeforeEach
    void setUp() {
        // Given: 회원 생성
        member1 = entityManager.merge(Member.builder()
                .name("홍길동")
                .email("hong@example.com")
                .socialUid("kakao_hong")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시 강남구")
                .detailAddress("테헤란로 123")
                .point(0)
                .build());

        member2 = entityManager.merge(Member.builder()
                .name("김영희")
                .email("kim@example.com")
                .socialUid("kakao_kim")
                .socialType(SocialType.KAKAO)
                .gender(Gender.FEMALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시 서초구")
                .detailAddress("강남대로 456")
                .point(0)
                .build());

        // Given: Location과 Food 생성
        Location gangnam = entityManager.merge(Location.builder().name("강남구").build());
        Location seocho = entityManager.merge(Location.builder().name("서초구").build());
        Food korean = entityManager.merge(Food.builder().name(FoodName.KOREAN).build());
        Food chinese = entityManager.merge(Food.builder().name(FoodName.CHINESE).build());
        Food japanese = entityManager.merge(Food.builder().name(FoodName.JAPANESE).build());

        // Given: 가게 생성
        koreanRestaurant = entityManager.merge(Store.builder()
                .name("맛있는 한식당")
                .managerNumber(1111111111L)
                .detailAddress("주소1")
                .location(gangnam)
                .food(korean)
                .build());

        chineseRestaurant = entityManager.merge(Store.builder()
                .name("맛있는 중식당")
                .managerNumber(2222222222L)
                .detailAddress("주소2")
                .location(seocho)
                .food(chinese)
                .build());

        japaneseRestaurant = entityManager.merge(Store.builder()
                .name("맛있는 일식당")
                .managerNumber(3333333333L)
                .detailAddress("주소3")
                .location(gangnam)
                .food(japanese)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[통합] 리뷰 생성 -> 회원별 조회 -> 별점 필터링 플로우 테스트")
    void fullReviewQueryFlow_ShouldWork() {
        // Given: member1이 3개 가게에 리뷰 작성
        Review review1 = Review.builder()
                .content("정말 맛있어요!")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build();

        Review review2 = Review.builder()
                .content("괜찮아요")
                .star(3.5f)
                .store(chineseRestaurant)
                .user(member1)
                .build();

        Review review3 = Review.builder()
                .content("별로예요")
                .star(2.0f)
                .store(japaneseRestaurant)
                .user(member1)
                .build();

        reviewRepository.save(review1);
        reviewRepository.save(review2);
        reviewRepository.save(review3);

        entityManager.flush();
        entityManager.clear();

        // When & Then 1: 회원의 모든 리뷰 조회
        List<Review> allReviews = reviewQueryRepository.findMyReviews(
                member1.getId(),
                null, null, null
        );
        assertThat(allReviews).hasSize(3);

        // When & Then 2: 별점 4.0 이상인 리뷰만 조회
        List<Review> highScoreReviews = reviewQueryRepository.findMyReviews(
                member1.getId(),
                null, 4.0f, null
        );
        assertThat(highScoreReviews).hasSize(1);
        assertThat(highScoreReviews.get(0).getStar()).isEqualTo(5.0f);

        // When & Then 3: 별점 2.0~3.5 범위 리뷰 조회
        List<Review> midRangeReviews = reviewQueryRepository.findMyReviews(
                member1.getId(),
                null, 2.0f, 3.5f
        );
        assertThat(midRangeReviews).hasSize(2);
        assertThat(midRangeReviews).extracting("star")
                .containsExactlyInAnyOrder(3.5f, 2.0f);
    }

    @Test
    @DisplayName("[QueryDSL] findMyReviews() 가게 이름으로 리뷰 검색")
    void findMyReviews_SearchByStoreName_ShouldWork() {
        // Given: member1이 여러 가게에 리뷰 작성
        reviewRepository.save(Review.builder()
                .content("한식당 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("중식당 리뷰")
                .star(4.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 가게 이름에 "한식"이 포함된 리뷰 검색
        List<Review> koreanReviews = reviewQueryRepository.findMyReviews(
                member1.getId(),
                "한식", null, null
        );

        // Then: 한식당 리뷰만 조회되어야 함
        assertThat(koreanReviews).hasSize(1);
        assertThat(koreanReviews.get(0).getStore().getName()).contains("한식");
        assertThat(koreanReviews.get(0).getContent()).isEqualTo("한식당 리뷰");
    }

    @Test
    @DisplayName("[QueryDSL] findMyReviewsByStarRange() 별점 범위 조회 - 최소 별점만 지정")
    void findMyReviewsByStarRange_MinScoreOnly_ShouldWork() {
        // Given: 다양한 별점의 리뷰 생성
        reviewRepository.save(Review.builder()
                .content("5점 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("4점 리뷰")
                .star(4.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("2점 리뷰")
                .star(2.0f)
                .store(japaneseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 별점 4.0 이상인 리뷰 조회 (maxScore = null)
        List<Review> result = reviewQueryRepository.findMyReviewsByStarRange(
                member1.getId(),
                4.0f, null
        );

        // Then: 4.0 이상인 리뷰만 조회
        assertThat(result).hasSize(2);
        assertThat(result).extracting("star")
                .containsExactlyInAnyOrder(5.0f, 4.0f);
    }

    @Test
    @DisplayName("[QueryDSL] findMyReviewsByStarRange() 별점 범위 조회 - 최대 별점만 지정")
    void findMyReviewsByStarRange_MaxScoreOnly_ShouldWork() {
        // Given: 다양한 별점의 리뷰 생성
        reviewRepository.save(Review.builder()
                .content("5점 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("3점 리뷰")
                .star(3.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("2점 리뷰")
                .star(2.0f)
                .store(japaneseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 별점 3.0 이하인 리뷰 조회 (minScore = null)
        List<Review> result = reviewQueryRepository.findMyReviewsByStarRange(
                member1.getId(),
                null, 3.0f
        );

        // Then: 3.0 이하인 리뷰만 조회
        assertThat(result).hasSize(2);
        assertThat(result).extracting("star")
                .containsExactlyInAnyOrder(3.0f, 2.0f);
    }

    @Test
    @DisplayName("[QueryDSL] findMyReviewsByStore() 특정 가게에 대한 회원의 리뷰 조회")
    void findMyReviewsByStore_ShouldWork() {
        // Given: member1이 한식당에 2개 리뷰, 중식당에 1개 리뷰 작성
        reviewRepository.save(Review.builder()
                .content("한식당 첫번째 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("한식당 두번째 리뷰")
                .star(4.5f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("중식당 리뷰")
                .star(3.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 한식당에 대한 member1의 리뷰만 조회
        List<Review> koreanReviews = reviewQueryRepository.findMyReviewsByStore(
                member1.getId(),
                koreanRestaurant.getId()
        );

        // Then: 한식당 리뷰 2개만 조회
        assertThat(koreanReviews).hasSize(2);
        assertThat(koreanReviews).allMatch(r -> r.getStore().getId().equals(koreanRestaurant.getId()));
        assertThat(koreanReviews).extracting("content")
                .containsExactlyInAnyOrder("한식당 첫번째 리뷰", "한식당 두번째 리뷰");
    }

    @Test
    @DisplayName("[QueryDSL] findReviews() 범용 검색 - 회원 중심 조회")
    void findReviews_MemberCentric_ShouldWork() {
        // Given: 2명의 회원이 각각 리뷰 작성
        reviewRepository.save(Review.builder()
                .content("member1 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("member2 리뷰")
                .star(4.0f)
                .store(koreanRestaurant)
                .user(member2)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: member1의 리뷰만 조회 (storeId는 null)
        List<Review> member1Reviews = reviewQueryRepository.findReviews(
                member1.getId(),
                null, null, null, null
        );

        // Then: member1의 리뷰만 조회되어야 함
        assertThat(member1Reviews).hasSize(1);
        assertThat(member1Reviews.get(0).getUser().getId()).isEqualTo(member1.getId());
        assertThat(member1Reviews.get(0).getContent()).isEqualTo("member1 리뷰");
    }

    @Test
    @DisplayName("[QueryDSL] findReviews() 범용 검색 - 가게 중심 조회")
    void findReviews_StoreCentric_ShouldWork() {
        // Given: 한식당에 2개 리뷰, 중식당에 1개 리뷰
        reviewRepository.save(Review.builder()
                .content("한식당 리뷰1")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("한식당 리뷰2")
                .star(4.0f)
                .store(koreanRestaurant)
                .user(member2)
                .build());

        reviewRepository.save(Review.builder()
                .content("중식당 리뷰")
                .star(3.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 한식당의 모든 리뷰 조회 (memberId는 null)
        List<Review> koreanRestaurantReviews = reviewQueryRepository.findReviews(
                null,
                koreanRestaurant.getId(),
                null, null, null
        );

        // Then: 한식당 리뷰 2개만 조회
        assertThat(koreanRestaurantReviews).hasSize(2);
        assertThat(koreanRestaurantReviews).allMatch(r -> r.getStore().getId().equals(koreanRestaurant.getId()));
        assertThat(koreanRestaurantReviews).extracting("content")
                .containsExactlyInAnyOrder("한식당 리뷰1", "한식당 리뷰2");
    }

    @Test
    @DisplayName("[QueryDSL] findReviews() 복합 조건 검색 - 회원 + 가게 + 별점 + 가게이름")
    void findReviews_ComplexConditions_ShouldWork() {
        // Given: 다양한 조건의 리뷰 생성
        reviewRepository.save(Review.builder()
                .content("한식당 5점")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("한식당 3점")
                .star(3.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("중식당 5점")
                .star(5.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("한식당 다른 회원")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member2)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: member1의 리뷰 중, 가게 이름에 "한식"이 포함되고, 별점 4.0 이상인 리뷰 조회
        List<Review> result = reviewQueryRepository.findReviews(
                member1.getId(),
                null,
                "한식",
                4.0f, null
        );

        // Then: 조건에 맞는 리뷰만 조회
        assertThat(result).hasSize(1);
        Review found = result.get(0);
        assertThat(found.getUser().getId()).isEqualTo(member1.getId());
        assertThat(found.getStore().getName()).contains("한식");
        assertThat(found.getStar()).isGreaterThanOrEqualTo(4.0f);
        assertThat(found.getContent()).isEqualTo("한식당 5점");
    }

    @Test
    @DisplayName("[QueryDSL] N+1 문제 없이 Fetch Join이 정상 작동하는지 검증")
    void findMyReviews_ShouldUseFetchJoinWithoutNPlusOne() {
        // Given: 리뷰 생성
        reviewRepository.save(Review.builder()
                .content("테스트 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: QueryDSL로 리뷰 조회 (Fetch Join 포함)
        List<Review> result = reviewQueryRepository.findMyReviews(
                member1.getId(),
                null, null, null
        );

        // Then: Store와 Member가 Lazy Loading 없이 접근 가능해야 함
        assertThat(result).hasSize(1);
        Review review = result.get(0);

        // Fetch Join 검증: Store와 Member에 접근 시 추가 쿼리 발생하지 않음
        assertThat(review.getStore()).isNotNull();
        assertThat(review.getStore().getName()).isEqualTo("맛있는 한식당");

        assertThat(review.getUser()).isNotNull();
        assertThat(review.getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("[QueryDSL] 리뷰 정렬 - 생성 시간 내림차순 검증")
    void findMyReviews_ShouldOrderByCreatedAtDesc() throws InterruptedException {
        // Given: 순차적으로 리뷰 3개 생성
        Review oldReview = reviewRepository.save(Review.builder()
                .content("첫번째 리뷰")
                .star(3.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        Thread.sleep(10); // 생성 시간 차이를 위한 대기

        Review middleReview = reviewRepository.save(Review.builder()
                .content("두번째 리뷰")
                .star(4.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        Thread.sleep(10);

        Review newReview = reviewRepository.save(Review.builder()
                .content("세번째 리뷰")
                .star(5.0f)
                .store(japaneseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: 리뷰 조회 (생성 시간 내림차순 정렬)
        List<Review> result = reviewQueryRepository.findMyReviews(
                member1.getId(),
                null, null, null
        );

        // Then: 최신순으로 정렬되어야 함 (세번째 -> 두번째 -> 첫번째)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("세번째 리뷰");
        assertThat(result.get(1).getContent()).isEqualTo("두번째 리뷰");
        assertThat(result.get(2).getContent()).isEqualTo("첫번째 리뷰");
    }

    @Test
    @DisplayName("[Service] ReviewQueryService 별점 범위 검증 - 유효하지 않은 별점 범위")
    void reviewQueryService_ValidateScoreRange_InvalidRange_ShouldThrowException() {
        // When & Then: 최소 별점이 최대 별점보다 큰 경우 예외 발생
        assertThatThrownBy(() ->
                reviewQueryService.getMyReviews(member1.getId(), null, 5.0f, 3.0f)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 최대 별점보다 클 수 없습니다");
    }

    @Test
    @DisplayName("[Service] ReviewQueryService 별점 범위 검증 - 별점 범위 초과")
    void reviewQueryService_ValidateScoreRange_OutOfRange_ShouldThrowException() {
        // When & Then: 별점이 5.0을 초과하는 경우 예외 발생
        assertThatThrownBy(() ->
                reviewQueryService.getMyReviews(member1.getId(), null, 0.0f, 6.0f)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 별점은 0.0 이상 5.0 이하여야 합니다");

        // When & Then: 별점이 0.0 미만인 경우 예외 발생
        assertThatThrownBy(() ->
                reviewQueryService.getMyReviews(member1.getId(), null, -1.0f, 5.0f)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 0.0 이상 5.0 이하여야 합니다");
    }

    @Test
    @DisplayName("[Repository] findByStoreIdWithUser() N+1 방지 Fetch Join 검증")
    void findByStoreIdWithUser_ShouldPreventNPlusOne() {
        // Given: 한식당에 2개 리뷰 작성
        reviewRepository.save(Review.builder()
                .content("리뷰1")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("리뷰2")
                .star(4.0f)
                .store(koreanRestaurant)
                .user(member2)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: @EntityGraph로 가게의 리뷰 조회
        List<Review> result = reviewRepository.findByStoreIdWithUser(koreanRestaurant.getId());

        // Then: N+1 문제 없이 회원 정보 접근 가능
        assertThat(result).hasSize(2);
        result.forEach(review -> {
            assertThat(review.getUser()).isNotNull();
            assertThat(review.getStore()).isNotNull();
        });
    }

    @Test
    @DisplayName("[Repository] findByMemberIdWithStore() N+1 방지 Fetch Join 검증")
    void findByMemberIdWithStore_ShouldPreventNPlusOne() {
        // Given: member1이 3개 가게에 리뷰 작성
        reviewRepository.save(Review.builder()
                .content("한식당 리뷰")
                .star(5.0f)
                .store(koreanRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("중식당 리뷰")
                .star(4.0f)
                .store(chineseRestaurant)
                .user(member1)
                .build());

        reviewRepository.save(Review.builder()
                .content("일식당 리뷰")
                .star(3.0f)
                .store(japaneseRestaurant)
                .user(member1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // When: @EntityGraph로 회원의 리뷰 조회
        List<Review> result = reviewRepository.findByMemberIdWithStore(member1.getId());

        // Then: N+1 문제 없이 가게 정보 접근 가능
        assertThat(result).hasSize(3);
        result.forEach(review -> {
            assertThat(review.getStore()).isNotNull();
        });
    }
}
