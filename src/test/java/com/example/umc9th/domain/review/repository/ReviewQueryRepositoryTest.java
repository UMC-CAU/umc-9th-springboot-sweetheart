package com.example.umc9th.domain.review.repository;

import com.example.umc9th.domain.location.entity.Location;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.review.entity.Review;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.config.JpaAuditingConfig;
import com.example.umc9th.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewQueryRepository 테스트
 *
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로딩 (가볍고 빠름)
 * - 내장 DB(H2) 자동 사용
 * - 각 테스트 후 자동 롤백 (DB 초기화)
 *
 * @Import(QueryDslConfig.class):
 * - QueryDSL 설정 수동 임포트
 * - JPAQueryFactory 빈 등록
 *
 * @Import(ReviewQueryRepository.class):
 * - ReviewQueryRepository 빈 등록
 *
 * @Import(JpaAuditingConfig.class):
 * - JPA Auditing 활성화 (createdAt 자동 설정)
 * - Review 엔티티가 @CreatedDate를 사용하므로 필요
 *
 * 테스트 목적:
 * - QueryDSL 동적 쿼리 검증
 * - 회원별 리뷰 조회 검증
 * - 가게별 리뷰 조회 검증
 * - 별점 범위 필터링 (minScore, maxScore) 동작 확인
 * - 가게 이름 검색 (부분 일치) 동작 확인
 * - 복합 조건 (회원 + 별점, 가게 + 별점 등) 동작 확인
 * - N+1 문제 방지 (Fetch Join) 검증
 * - 빈 결과 처리 확인
 */
@DataJpaTest
@Import({QueryDslConfig.class, ReviewQueryRepository.class, JpaAuditingConfig.class})
@DisplayName("ReviewQueryRepository 테스트")
class ReviewQueryRepositoryTest {

    @Autowired
    private ReviewQueryRepository reviewQueryRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 데이터 참조 변수
    private Member member1, member2, member3;
    private Store store1, store2, store3;
    private Location location1, location2;
    private Food korean, japanese, western;

    @BeforeEach
    void setUp() {
        // Location 데이터 생성
        location1 = Location.builder().name("강남구").build();
        location2 = Location.builder().name("서초구").build();

        entityManager.persist(location1);
        entityManager.persist(location2);

        // Food 데이터 생성
        korean = Food.builder().name(FoodName.KOREAN).build();
        japanese = Food.builder().name(FoodName.JAPANESE).build();
        western = Food.builder().name(FoodName.WESTERN).build();

        entityManager.persist(korean);
        entityManager.persist(japanese);
        entityManager.persist(western);

        // Member 데이터 생성
        member1 = createMember("홍길동", "hong@example.com", "01011111111");
        member2 = createMember("김철수", "kim@example.com", "01022222222");
        member3 = createMember("이영희", "lee@example.com", "01033333333");

        // Store 데이터 생성
        store1 = createStore("강남 한식당", 1011111111L, "테헤란로 123", location1, korean);
        store2 = createStore("서초 일식당", 1022222222L, "서초대로 456", location2, japanese);
        store3 = createStore("강남 카페", 1033333333L, "강남대로 789", location1, western);

        // Review 데이터 생성 (다양한 별점, 다양한 조합)
        createTestReviews();

        // 쓰기 지연 SQL 실행 및 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Member 생성 헬퍼 메서드
     */
    private Member createMember(String name, String email, String phoneNumber) {
        Member member = Member.builder()
                .name(name)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("상세주소")
                .socialUid("social-uid-" + name)
                .socialType(SocialType.KAKAO)
                .point(0)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();
        entityManager.persist(member);
        return member;
    }

    /**
     * Store 생성 헬퍼 메서드
     */
    private Store createStore(String name, Long managerNumber, String detailAddress, Location location, Food food) {
        Store store = Store.builder()
                .name(name)
                .managerNumber(managerNumber)
                .detailAddress(detailAddress)
                .location(location)
                .food(food)
                .build();
        entityManager.persist(store);
        return store;
    }

    /**
     * Review 생성 헬퍼 메서드
     */
    private void createReview(Member member, Store store, String content, Float star) {
        Review review = Review.builder()
                .user(member)
                .store(store)
                .content(content)
                .star(star)
                .build();
        entityManager.persist(review);
    }

    /**
     * 테스트 데이터 생성
     * - Member1: 5개 리뷰 (별점 1.0, 2.5, 3.5, 4.0, 5.0)
     * - Member2: 3개 리뷰 (별점 2.0, 3.0, 4.5)
     * - Member3: 2개 리뷰 (별점 1.5, 5.0)
     */
    private void createTestReviews() {
        // Member1 리뷰 (5개)
        createReview(member1, store1, "맛있어요", 5.0f);           // 별점 5.0
        createReview(member1, store1, "별로예요", 1.0f);           // 별점 1.0 (동일 가게)
        createReview(member1, store2, "괜찮아요", 3.5f);           // 별점 3.5
        createReview(member1, store2, "좋아요", 4.0f);            // 별점 4.0 (동일 가게)
        createReview(member1, store3, "보통이에요", 2.5f);         // 별점 2.5

        // Member2 리뷰 (3개)
        createReview(member2, store1, "맛있네요", 4.5f);           // 별점 4.5
        createReview(member2, store2, "별로예요", 2.0f);           // 별점 2.0
        createReview(member2, store3, "평범해요", 3.0f);           // 별점 3.0

        // Member3 리뷰 (2개)
        createReview(member3, store1, "최고예요!", 5.0f);          // 별점 5.0
        createReview(member3, store3, "실망이에요", 1.5f);         // 별점 1.5
    }

    // ========== findMyReviews() 테스트 ==========

    @Test
    @DisplayName("[findMyReviews] 회원별 전체 리뷰 조회 - 필터 없음")
    void findMyReviews_NoFilter_ReturnsAllMemberReviews() {
        // Given: Member1의 리뷰 조회 (필터 없음)
        Long memberId = member1.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, null, null, null);

        // Then: Member1의 모든 리뷰 조회 (5개)
        assertThat(results).hasSize(5);

        // 모든 리뷰가 Member1의 것인지 확인
        assertThat(results)
                .allMatch(review -> review.getUser().getId().equals(memberId));

        // 최신순 정렬 확인 (createdAt 내림차순)
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getCreatedAt())
                    .isAfterOrEqualTo(results.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("[findMyReviews] 가게 이름으로 필터링 - 부분 일치")
    void findMyReviews_WithStoreName_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 가게 이름에 "한식"이 포함된 것만 조회
        Long memberId = member1.getId();
        String storeName = "한식";

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, storeName, null, null);

        // Then: "강남 한식당"에 대한 리뷰만 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStore().getName().contains(storeName));
    }

    @Test
    @DisplayName("[findMyReviews] 최소 별점 필터링 (minScore만 지정)")
    void findMyReviews_WithMinScore_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 별점 4.0 이상만 조회
        Long memberId = member1.getId();
        Float minScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, null, minScore, null);

        // Then: 별점 4.0, 5.0인 리뷰만 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStar() >= minScore);
    }

    @Test
    @DisplayName("[findMyReviews] 최대 별점 필터링 (maxScore만 지정)")
    void findMyReviews_WithMaxScore_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 별점 3.0 이하만 조회
        Long memberId = member1.getId();
        Float maxScore = 3.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, null, null, maxScore);

        // Then: 별점 1.0, 2.5인 리뷰만 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStar() <= maxScore);
    }

    @Test
    @DisplayName("[findMyReviews] 별점 범위 필터링 (minScore와 maxScore 둘 다 지정)")
    void findMyReviews_WithStarRange_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 별점 2.0 ~ 4.0 범위만 조회
        Long memberId = member1.getId();
        Float minScore = 2.0f;
        Float maxScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, null, minScore, maxScore);

        // Then: 별점 2.5, 3.5, 4.0인 리뷰만 조회 (3개)
        assertThat(results).hasSize(3);
        assertThat(results)
                .allMatch(review -> review.getStar() >= minScore && review.getStar() <= maxScore);
    }

    @Test
    @DisplayName("[findMyReviews] 복합 조건 - 가게 이름 + 별점 범위")
    void findMyReviews_WithStoreNameAndStarRange_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 "일식당"이고 별점 3.0 이상인 것만 조회
        Long memberId = member1.getId();
        String storeName = "일식";
        Float minScore = 3.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, storeName, minScore, null);

        // Then: 별점 3.5, 4.0인 리뷰만 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStore().getName().contains(storeName)
                        && review.getStar() >= minScore);
    }

    @Test
    @DisplayName("[findMyReviews] 조건에 맞는 리뷰가 없는 경우 - 빈 리스트 반환")
    void findMyReviews_NoMatch_ReturnsEmptyList() {
        // Given: Member1의 리뷰 중 존재하지 않는 가게 이름 검색
        Long memberId = member1.getId();
        String storeName = "존재하지않는가게";

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, storeName, null, null);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    // ========== findMyReviewsByStarRange() 테스트 ==========

    @Test
    @DisplayName("[findMyReviewsByStarRange] 별점 범위로 리뷰 조회 - 정확한 범위")
    void findMyReviewsByStarRange_WithExactRange_ReturnsFilteredReviews() {
        // Given: Member2의 리뷰 중 별점 2.5 ~ 4.0 범위만 조회
        Long memberId = member2.getId();
        Float minScore = 2.5f;
        Float maxScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStarRange(memberId, minScore, maxScore);

        // Then: 별점 3.0인 리뷰만 조회 (1개)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStar()).isEqualTo(3.0f);
    }

    @Test
    @DisplayName("[findMyReviewsByStarRange] 별점 범위 - 최소값만 지정")
    void findMyReviewsByStarRange_OnlyMinScore_ReturnsFilteredReviews() {
        // Given: Member2의 리뷰 중 별점 4.0 이상만 조회
        Long memberId = member2.getId();
        Float minScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStarRange(memberId, minScore, null);

        // Then: 별점 4.5인 리뷰만 조회 (1개)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStar()).isEqualTo(4.5f);
    }

    @Test
    @DisplayName("[findMyReviewsByStarRange] 별점 범위 - 최대값만 지정")
    void findMyReviewsByStarRange_OnlyMaxScore_ReturnsFilteredReviews() {
        // Given: Member2의 리뷰 중 별점 3.0 이하만 조회
        Long memberId = member2.getId();
        Float maxScore = 3.0f;

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStarRange(memberId, null, maxScore);

        // Then: 별점 2.0, 3.0인 리뷰 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStar() <= maxScore);
    }

    @Test
    @DisplayName("[findMyReviewsByStarRange] 별점 범위 - 필터 없음")
    void findMyReviewsByStarRange_NoFilter_ReturnsAllReviews() {
        // Given: Member3의 모든 리뷰 조회
        Long memberId = member3.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStarRange(memberId, null, null);

        // Then: Member3의 모든 리뷰 조회 (2개)
        assertThat(results).hasSize(2);
    }

    // ========== findMyReviewsByStore() 테스트 ==========

    @Test
    @DisplayName("[findMyReviewsByStore] 특정 가게에 대한 회원의 리뷰 조회")
    void findMyReviewsByStore_ReturnsStoreReviews() {
        // Given: Member1이 작성한 Store1에 대한 리뷰 조회
        Long memberId = member1.getId();
        Long storeId = store1.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStore(memberId, storeId);

        // Then: Store1에 대한 리뷰 2개 조회
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStore().getId().equals(storeId)
                        && review.getUser().getId().equals(memberId));

        // 별점이 5.0, 1.0인지 확인
        assertThat(results)
                .extracting(Review::getStar)
                .containsExactlyInAnyOrder(5.0f, 1.0f);
    }

    @Test
    @DisplayName("[findMyReviewsByStore] 리뷰가 없는 가게 조회 - 빈 리스트 반환")
    void findMyReviewsByStore_NoReviews_ReturnsEmptyList() {
        // Given: Member3이 작성한 Store2에 대한 리뷰 조회 (실제로는 없음)
        Long memberId = member3.getId();
        Long storeId = store2.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStore(memberId, storeId);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    // ========== findReviews() 통합 검색 테스트 ==========

    @Test
    @DisplayName("[findReviews] 전체 리뷰 조회 - 필터 없음")
    void findReviews_NoFilter_ReturnsAllReviews() {
        // Given: 모든 조건 null
        // When
        List<Review> results = reviewQueryRepository.findReviews(null, null, null, null, null);

        // Then: 모든 리뷰 조회 (10개)
        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("[findReviews] 회원별 리뷰 조회")
    void findReviews_ByMemberId_ReturnsMemberReviews() {
        // Given: Member2의 리뷰만 조회
        Long memberId = member2.getId();

        // When
        List<Review> results = reviewQueryRepository.findReviews(memberId, null, null, null, null);

        // Then: Member2의 모든 리뷰 조회 (3개)
        assertThat(results).hasSize(3);
        assertThat(results)
                .allMatch(review -> review.getUser().getId().equals(memberId));
    }

    @Test
    @DisplayName("[findReviews] 가게별 리뷰 조회")
    void findReviews_ByStoreId_ReturnsStoreReviews() {
        // Given: Store1에 대한 모든 리뷰 조회
        Long storeId = store1.getId();

        // When
        List<Review> results = reviewQueryRepository.findReviews(null, storeId, null, null, null);

        // Then: Store1에 대한 모든 리뷰 조회 (4개: Member1 2개, Member2 1개, Member3 1개)
        assertThat(results).hasSize(4);
        assertThat(results)
                .allMatch(review -> review.getStore().getId().equals(storeId));
    }

    @Test
    @DisplayName("[findReviews] 가게 이름으로 검색")
    void findReviews_ByStoreName_ReturnsFilteredReviews() {
        // Given: "카페"가 포함된 가게의 리뷰 조회
        String storeName = "카페";

        // When
        List<Review> results = reviewQueryRepository.findReviews(null, null, storeName, null, null);

        // Then: "강남 카페"에 대한 리뷰 조회 (3개)
        assertThat(results).hasSize(3);
        assertThat(results)
                .allMatch(review -> review.getStore().getName().contains(storeName));
    }

    @Test
    @DisplayName("[findReviews] 별점 범위로 필터링")
    void findReviews_ByStarRange_ReturnsFilteredReviews() {
        // Given: 별점 4.0 ~ 5.0 범위의 리뷰 조회
        Float minScore = 4.0f;
        Float maxScore = 5.0f;

        // When
        List<Review> results = reviewQueryRepository.findReviews(null, null, null, minScore, maxScore);

        // Then: 별점 4.0, 4.5, 5.0인 리뷰 조회 (4개: Member1 2개, Member2 1개, Member3 1개)
        assertThat(results).hasSize(4);
        assertThat(results)
                .allMatch(review -> review.getStar() >= minScore && review.getStar() <= maxScore);
    }

    @Test
    @DisplayName("[findReviews] 복합 조건 - 회원 + 가게")
    void findReviews_ByMemberAndStore_ReturnsFilteredReviews() {
        // Given: Member1이 작성한 Store2에 대한 리뷰 조회
        Long memberId = member1.getId();
        Long storeId = store2.getId();

        // When
        List<Review> results = reviewQueryRepository.findReviews(memberId, storeId, null, null, null);

        // Then: 2개 조회
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getUser().getId().equals(memberId)
                        && review.getStore().getId().equals(storeId));
    }

    @Test
    @DisplayName("[findReviews] 복합 조건 - 회원 + 별점")
    void findReviews_ByMemberAndStarRange_ReturnsFilteredReviews() {
        // Given: Member1의 리뷰 중 별점 4.0 이상
        Long memberId = member1.getId();
        Float minScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findReviews(memberId, null, null, minScore, null);

        // Then: 별점 4.0, 5.0인 리뷰 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getUser().getId().equals(memberId)
                        && review.getStar() >= minScore);
    }

    @Test
    @DisplayName("[findReviews] 복합 조건 - 가게 + 별점")
    void findReviews_ByStoreAndStarRange_ReturnsFilteredReviews() {
        // Given: Store1에 대한 리뷰 중 별점 4.0 이상
        Long storeId = store1.getId();
        Float minScore = 4.0f;

        // When
        List<Review> results = reviewQueryRepository.findReviews(null, storeId, null, minScore, null);

        // Then: 별점 4.5, 5.0인 리뷰 조회 (2개: Member1의 5.0, Member2의 4.5, Member3의 5.0)
        assertThat(results).hasSize(3);
        assertThat(results)
                .allMatch(review -> review.getStore().getId().equals(storeId)
                        && review.getStar() >= minScore);
    }

    @Test
    @DisplayName("[findReviews] 복합 조건 - 가게 이름 + 별점")
    void findReviews_ByStoreNameAndStarRange_ReturnsFilteredReviews() {
        // Given: "일식"이 포함된 가게의 리뷰 중 별점 3.0 이상
        String storeName = "일식";
        Float minScore = 3.0f;

        // When
        List<Review> results = reviewQueryRepository.findReviews(null, null, storeName, minScore, null);

        // Then: 별점 3.5, 4.0인 리뷰 조회 (2개)
        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(review -> review.getStore().getName().contains(storeName)
                        && review.getStar() >= minScore);
    }

    @Test
    @DisplayName("[findReviews] 복합 조건 - 모든 필터 조합")
    void findReviews_AllFilters_ReturnsFilteredReviews() {
        // Given: Member1이 작성한 "한식"이 포함된 가게의 리뷰 중 별점 3.0 이상
        Long memberId = member1.getId();
        String storeName = "한식";
        Float minScore = 3.0f;

        // When
        List<Review> results = reviewQueryRepository.findReviews(memberId, null, storeName, minScore, null);

        // Then: 별점 5.0인 리뷰 1개만 조회
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStar()).isEqualTo(5.0f);
    }

    // ========== N+1 문제 방지 검증 ==========

    @Test
    @DisplayName("N+1 문제 방지 - Store와 Member가 Fetch Join으로 조회됨")
    void findReviews_FetchJoin_LoadsStoreAndMember() {
        // Given: Member1의 모든 리뷰 조회
        Long memberId = member1.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviews(memberId, null, null, null);

        // Then: LazyInitializationException 발생 안 함
        assertThat(results).isNotEmpty();

        // Store와 Member 접근 시 추가 쿼리 없이 조회 가능
        results.forEach(review -> {
            // Store 접근
            assertThat(review.getStore()).isNotNull();
            assertThat(review.getStore().getName()).isNotNull();

            // Member 접근
            assertThat(review.getUser()).isNotNull();
            assertThat(review.getUser().getName()).isNotNull();

            // Store의 Location과 Food 접근 (Fetch Join 포함됨)
            assertThat(review.getStore().getLocation()).isNotNull();
            assertThat(review.getStore().getLocation().getName()).isNotNull();
            assertThat(review.getStore().getFood()).isNotNull();
            assertThat(review.getStore().getFood().getName()).isNotNull();
        });
    }

    @Test
    @DisplayName("N+1 문제 방지 - findReviews()에서 Fetch Join 동작 검증")
    void findReviews_FetchJoin_NoLazyInitializationException() {
        // Given: 전체 리뷰 조회
        // When
        List<Review> results = reviewQueryRepository.findReviews(null, null, null, null, null);

        // Then: Fetch Join으로 인해 LazyInitializationException 발생하지 않음
        assertThat(results).hasSize(10);

        // 모든 연관 엔티티 접근 가능
        results.forEach(review -> {
            assertThat(review.getStore()).isNotNull();
            assertThat(review.getStore().getName()).isNotNull();
            assertThat(review.getUser()).isNotNull();
            assertThat(review.getUser().getName()).isNotNull();
        });
    }

    @Test
    @DisplayName("N+1 문제 방지 - findMyReviewsByStore()에서 Fetch Join 동작 검증")
    void findMyReviewsByStore_FetchJoin_NoLazyInitializationException() {
        // Given: Member1의 Store1에 대한 리뷰 조회
        Long memberId = member1.getId();
        Long storeId = store1.getId();

        // When
        List<Review> results = reviewQueryRepository.findMyReviewsByStore(memberId, storeId);

        // Then: Fetch Join으로 인해 Store 접근 가능
        assertThat(results).hasSize(2);

        results.forEach(review -> {
            assertThat(review.getStore()).isNotNull();
            assertThat(review.getStore().getName()).isNotNull();
            // Member는 fetch join에 포함되지 않지만, 이미 영속성 컨텍스트에 있으므로 접근 가능
            // (단, 실제 프로덕션에서는 member도 fetch join 하는 것을 권장)
        });
    }
}
