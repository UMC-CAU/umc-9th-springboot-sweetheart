package com.example.umc9th.domain.review.service;

import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.entity.Review;
import com.example.umc9th.domain.review.repository.ReviewQueryRepository;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.global.auth.enums.SocialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ReviewQueryService 테스트
 *
 * @ExtendWith(MockitoExtension.class):
 * - Mockito를 사용한 단위 테스트
 * - 실제 DB를 사용하지 않음 (Mock 객체 사용)
 * - 매우 빠르게 실행됨
 *
 * Mock vs Real:
 * - Mock: 가짜 객체, "이렇게 동작한다고 가정"
 * - Real: 실제 객체, 진짜 DB에 저장/조회
 *
 * 왜 Service는 Mock으로 테스트할까?
 * - Service는 비즈니스 로직만 테스트하고 싶음
 * - Repository가 제대로 동작한다고 가정 (Repository 테스트는 따로 함)
 * - DB 없이 빠르게 테스트 가능
 *
 * ReviewQueryService 특징:
 * - Read-only 서비스 (@Transactional(readOnly = true))
 * - QueryDSL을 사용한 복잡한 동적 쿼리 처리
 * - 별점 범위 검증 로직 포함
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewQueryService 테스트")
class ReviewQueryServiceTest {

    /**
     * @Mock: 가짜 객체 생성
     * - 실제 ReviewQueryRepository가 아닌 Mock 객체
     * - given()으로 동작 방식을 정의해줘야 함
     */
    @Mock
    private ReviewQueryRepository reviewQueryRepository;

    /**
     * @InjectMocks: @Mock 객체들을 자동 주입
     * - ReviewQueryService의 생성자에 reviewQueryRepository를 자동 주입
     * - 실제로 테스트할 대상 객체
     */
    @InjectMocks
    private ReviewQueryService reviewQueryService;


    // ===== getMyReviews() 테스트 =====

    /**
     * getMyReviews() 정상 조회 테스트 - 모든 필터 적용
     *
     * BDD 스타일 (Behavior-Driven Development):
     * - given(): "이런 상황이 주어졌을 때"
     * - when(): "이걸 실행하면"
     * - then(): "이런 결과가 나와야 한다"
     */
    @Test
    @DisplayName("회원의 리뷰를 모든 필터 조건(가게명, 별점 범위)으로 조회할 수 있다")
    void getMyReviews_AllFilters_Success() {
        // Given: Repository가 리뷰 리스트를 반환한다고 가정
        Long memberId = 1L;
        String storeName = "맛집";
        Float minScore = 3.0f;
        Float maxScore = 5.0f;

        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "정말 맛있어요", 4.5f, memberId, "맛집1"),
                createMockReview(2L, "괜찮아요", 3.5f, memberId, "맛집2")
        );

        // Mockito: "findMyReviews()가 호출되면 mockReviews를 반환해라"
        given(reviewQueryRepository.findMyReviews(memberId, storeName, minScore, maxScore))
                .willReturn(mockReviews);

        // When: Service의 getMyReviews() 호출
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviews(memberId, storeName, minScore, maxScore);

        // Then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("정말 맛있어요");
        assertThat(result.get(0).getStar()).isEqualTo(4.5f);
        assertThat(result.get(1).getContent()).isEqualTo("괜찮아요");
        assertThat(result.get(1).getStar()).isEqualTo(3.5f);

        // 검증: Repository의 findMyReviews가 1번 호출되었는지
        then(reviewQueryRepository).should(times(1))
                .findMyReviews(memberId, storeName, minScore, maxScore);
    }

    /**
     * getMyReviews() - 필터 없이 조회
     */
    @Test
    @DisplayName("회원의 리뷰를 필터 없이 전체 조회할 수 있다")
    void getMyReviews_NoFilters_Success() {
        // Given: 모든 필터가 null
        Long memberId = 1L;

        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "리뷰1", 5.0f, memberId, "가게1"),
                createMockReview(2L, "리뷰2", 2.0f, memberId, "가게2"),
                createMockReview(3L, "리뷰3", 4.0f, memberId, "가게3")
        );

        given(reviewQueryRepository.findMyReviews(memberId, null, null, null))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviews(memberId, null, null, null);

        // Then: 모든 리뷰 반환
        assertThat(result).hasSize(3);
        then(reviewQueryRepository).should().findMyReviews(memberId, null, null, null);
    }

    /**
     * getMyReviews() - 빈 결과 처리
     */
    @Test
    @DisplayName("조건에 맞는 리뷰가 없으면 빈 리스트를 반환한다")
    void getMyReviews_EmptyResult() {
        // Given: Repository가 빈 리스트 반환
        Long memberId = 1L;
        given(reviewQueryRepository.findMyReviews(eq(memberId), any(), any(), any()))
                .willReturn(Collections.emptyList());

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviews(memberId, "존재하지않는가게", 4.0f, 5.0f);

        // Then: 빈 리스트여야 함 (예외 발생 X)
        assertThat(result).isEmpty();
    }


    // ===== 별점 범위 검증 테스트 (중요!) =====

    /**
     * getMyReviews() - 최소 별점이 0보다 작으면 예외 발생
     *
     * 별점 검증 테스트는 매우 중요!
     * - validateScoreRange()는 private 메서드이지만
     * - public 메서드(getMyReviews)를 통해 간접 테스트
     */
    @Test
    @DisplayName("최소 별점이 0보다 작으면 IllegalArgumentException이 발생한다")
    void getMyReviews_MinScoreLessThanZero_ThrowsException() {
        // Given: minScore < 0
        Long memberId = 1L;
        Float invalidMinScore = -1.0f;
        Float maxScore = 5.0f;

        // When & Then: IllegalArgumentException 발생 확인
        assertThatThrownBy(() -> reviewQueryService.getMyReviews(memberId, null, invalidMinScore, maxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 0.0 이상 5.0 이하여야 합니다");

        // Repository는 호출되지 않아야 함 (검증 단계에서 실패했으므로)
        then(reviewQueryRepository).should(never()).findMyReviews(any(), any(), any(), any());
    }

    /**
     * getMyReviews() - 최소 별점이 5보다 크면 예외 발생
     */
    @Test
    @DisplayName("최소 별점이 5보다 크면 IllegalArgumentException이 발생한다")
    void getMyReviews_MinScoreGreaterThanFive_ThrowsException() {
        // Given: minScore > 5
        Long memberId = 1L;
        Float invalidMinScore = 6.0f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getMyReviews(memberId, null, invalidMinScore, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 0.0 이상 5.0 이하여야 합니다");

        then(reviewQueryRepository).should(never()).findMyReviews(any(), any(), any(), any());
    }

    /**
     * getMyReviews() - 최대 별점이 0보다 작으면 예외 발생
     */
    @Test
    @DisplayName("최대 별점이 0보다 작으면 IllegalArgumentException이 발생한다")
    void getMyReviews_MaxScoreLessThanZero_ThrowsException() {
        // Given: maxScore < 0
        Long memberId = 1L;
        Float invalidMaxScore = -0.5f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getMyReviews(memberId, null, null, invalidMaxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 별점은 0.0 이상 5.0 이하여야 합니다");

        then(reviewQueryRepository).should(never()).findMyReviews(any(), any(), any(), any());
    }

    /**
     * getMyReviews() - 최대 별점이 5보다 크면 예외 발생
     */
    @Test
    @DisplayName("최대 별점이 5보다 크면 IllegalArgumentException이 발생한다")
    void getMyReviews_MaxScoreGreaterThanFive_ThrowsException() {
        // Given: maxScore > 5
        Long memberId = 1L;
        Float minScore = 3.0f;
        Float invalidMaxScore = 7.0f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getMyReviews(memberId, null, minScore, invalidMaxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 별점은 0.0 이상 5.0 이하여야 합니다");

        then(reviewQueryRepository).should(never()).findMyReviews(any(), any(), any(), any());
    }

    /**
     * getMyReviews() - 최소 별점이 최대 별점보다 크면 예외 발생
     *
     * 가장 중요한 검증 로직!
     */
    @Test
    @DisplayName("최소 별점이 최대 별점보다 크면 IllegalArgumentException이 발생한다")
    void getMyReviews_MinScoreGreaterThanMaxScore_ThrowsException() {
        // Given: minScore > maxScore (논리적으로 불가능한 범위)
        Long memberId = 1L;
        Float minScore = 4.5f;
        Float maxScore = 2.0f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getMyReviews(memberId, null, minScore, maxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 최대 별점보다 클 수 없습니다");

        then(reviewQueryRepository).should(never()).findMyReviews(any(), any(), any(), any());
    }

    /**
     * getMyReviews() - 유효한 경계값 테스트 (0.0 ~ 5.0)
     */
    @Test
    @DisplayName("별점 범위가 0.0 ~ 5.0 사이면 정상 조회된다")
    void getMyReviews_ValidScoreRange_Success() {
        // Given: 유효한 경계값 (0.0 ~ 5.0)
        Long memberId = 1L;
        Float minScore = 0.0f;  // 최소 경계값
        Float maxScore = 5.0f;  // 최대 경계값

        given(reviewQueryRepository.findMyReviews(memberId, null, minScore, maxScore))
                .willReturn(Collections.emptyList());

        // When: 예외 없이 실행되어야 함
        assertThatCode(() -> reviewQueryService.getMyReviews(memberId, null, minScore, maxScore))
                .doesNotThrowAnyException();

        // Then
        then(reviewQueryRepository).should().findMyReviews(memberId, null, minScore, maxScore);
    }


    // ===== getMyReviewsByStarRange() 테스트 =====

    /**
     * getMyReviewsByStarRange() 정상 조회 테스트
     */
    @Test
    @DisplayName("회원의 리뷰를 별점 범위로 조회할 수 있다")
    void getMyReviewsByStarRange_Success() {
        // Given
        Long memberId = 1L;
        Float minScore = 3.0f;
        Float maxScore = 4.5f;

        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "좋아요", 4.0f, memberId, "가게1"),
                createMockReview(2L, "괜찮아요", 3.5f, memberId, "가게2")
        );

        given(reviewQueryRepository.findMyReviewsByStarRange(memberId, minScore, maxScore))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviewsByStarRange(memberId, minScore, maxScore);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("star")
                .containsExactly(4.0f, 3.5f);

        then(reviewQueryRepository).should().findMyReviewsByStarRange(memberId, minScore, maxScore);
    }

    /**
     * getMyReviewsByStarRange() - 잘못된 별점 범위
     */
    @Test
    @DisplayName("별점 범위가 잘못되면 IllegalArgumentException이 발생한다")
    void getMyReviewsByStarRange_InvalidRange_ThrowsException() {
        // Given: minScore > maxScore
        Long memberId = 1L;
        Float minScore = 4.0f;
        Float maxScore = 2.0f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getMyReviewsByStarRange(memberId, minScore, maxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 최대 별점보다 클 수 없습니다");
    }


    // ===== getMyReviewsByStore() 테스트 =====

    /**
     * getMyReviewsByStore() 정상 조회 테스트
     */
    @Test
    @DisplayName("특정 가게에 대한 회원의 리뷰를 조회할 수 있다")
    void getMyReviewsByStore_Success() {
        // Given
        Long memberId = 1L;
        Long storeId = 10L;

        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "첫 번째 방문", 5.0f, memberId, "가게A"),
                createMockReview(2L, "두 번째 방문", 4.5f, memberId, "가게A"),
                createMockReview(3L, "세 번째 방문", 4.0f, memberId, "가게A")
        );

        given(reviewQueryRepository.findMyReviewsByStore(memberId, storeId))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviewsByStore(memberId, storeId);

        // Then: 같은 가게에 대한 여러 리뷰
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("content")
                .containsExactly("첫 번째 방문", "두 번째 방문", "세 번째 방문");

        then(reviewQueryRepository).should().findMyReviewsByStore(memberId, storeId);
    }

    /**
     * getMyReviewsByStore() - 빈 결과
     */
    @Test
    @DisplayName("특정 가게에 대한 리뷰가 없으면 빈 리스트를 반환한다")
    void getMyReviewsByStore_EmptyResult() {
        // Given
        Long memberId = 1L;
        Long storeId = 999L;

        given(reviewQueryRepository.findMyReviewsByStore(memberId, storeId))
                .willReturn(Collections.emptyList());

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviewsByStore(memberId, storeId);

        // Then
        assertThat(result).isEmpty();
    }


    // ===== getReviews() 통합 테스트 =====

    /**
     * getReviews() - 모든 필터 조건 적용
     *
     * 범용 API 메서드 테스트
     */
    @Test
    @DisplayName("범용 리뷰 조회에서 모든 필터 조건을 적용할 수 있다")
    void getReviews_AllFilters_Success() {
        // Given: 모든 필터 조건 설정
        Long memberId = 1L;
        Long storeId = 10L;
        String storeName = "맛집";
        Float minScore = 3.0f;
        Float maxScore = 5.0f;

        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "완벽해요", 5.0f, memberId, "맛집A"),
                createMockReview(2L, "좋아요", 4.0f, memberId, "맛집B")
        );

        given(reviewQueryRepository.findReviews(memberId, storeId, storeName, minScore, maxScore))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(memberId, storeId, storeName, minScore, maxScore);

        // Then
        assertThat(result).hasSize(2);
        then(reviewQueryRepository).should().findReviews(memberId, storeId, storeName, minScore, maxScore);
    }

    /**
     * getReviews() - 일부 필터만 적용
     */
    @Test
    @DisplayName("범용 리뷰 조회에서 일부 필터만 적용할 수 있다")
    void getReviews_PartialFilters_Success() {
        // Given: memberId와 minScore만 설정 (storeId, storeName, maxScore는 null)
        Long memberId = 1L;
        Float minScore = 4.0f;

        List<Review> mockReviews = List.of(
                createMockReview(1L, "좋아요", 4.5f, memberId, "가게1")
        );

        given(reviewQueryRepository.findReviews(memberId, null, null, minScore, null))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(memberId, null, null, minScore, null);

        // Then
        assertThat(result).hasSize(1);
        then(reviewQueryRepository).should().findReviews(memberId, null, null, minScore, null);
    }

    /**
     * getReviews() - 별점 검증 실패
     */
    @Test
    @DisplayName("범용 리뷰 조회에서도 별점 범위 검증이 적용된다")
    void getReviews_InvalidScoreRange_ThrowsException() {
        // Given: 잘못된 별점 범위
        Long memberId = 1L;
        Float minScore = 5.0f;
        Float maxScore = 1.0f;

        // When & Then
        assertThatThrownBy(() -> reviewQueryService.getReviews(memberId, null, null, minScore, maxScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 별점은 최대 별점보다 클 수 없습니다");

        then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
    }

    /**
     * getReviews() - 필터 없이 전체 조회
     */
    @Test
    @DisplayName("범용 리뷰 조회에서 필터 없이 전체 조회할 수 있다")
    void getReviews_NoFilters_Success() {
        // Given: 모든 필터가 null
        List<Review> mockReviews = Arrays.asList(
                createMockReview(1L, "리뷰1", 5.0f, 1L, "가게1"),
                createMockReview(2L, "리뷰2", 3.0f, 2L, "가게2"),
                createMockReview(3L, "리뷰3", 4.0f, 3L, "가게3")
        );

        given(reviewQueryRepository.findReviews(null, null, null, null, null))
                .willReturn(mockReviews);

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(null, null, null, null, null);

        // Then: 모든 리뷰 반환
        assertThat(result).hasSize(3);
    }


    // ===== DTO 변환 검증 테스트 =====

    /**
     * DTO 변환이 올바르게 수행되는지 검증
     *
     * Service는 엔티티를 DTO로 변환하는 책임도 가지므로
     * 변환 로직이 제대로 동작하는지 확인 필요
     */
    @Test
    @DisplayName("Review 엔티티가 ReviewResponse.MyReview DTO로 올바르게 변환된다")
    void entityToDtoConversion_Success() {
        // Given
        Long memberId = 1L;
        Review mockReview = createMockReview(1L, "변환 테스트", 4.5f, memberId, "테스트가게");

        given(reviewQueryRepository.findMyReviews(eq(memberId), any(), any(), any()))
                .willReturn(List.of(mockReview));

        // When
        List<ReviewResponse.MyReview> result = reviewQueryService.getMyReviews(memberId, null, null, null);

        // Then: DTO 필드가 엔티티 필드와 일치하는지 확인
        assertThat(result).hasSize(1);
        ReviewResponse.MyReview dto = result.get(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getContent()).isEqualTo("변환 테스트");
        assertThat(dto.getStar()).isEqualTo(4.5f);
        assertThat(dto.getCreatedAt()).isNotNull();

        // Store 정보도 포함되는지 확인
        assertThat(dto.getStore()).isNotNull();
        assertThat(dto.getStore().getName()).isEqualTo("테스트가게");
    }


    // ===== 헬퍼 메서드 =====

    /**
     * Mock Review 생성 헬퍼 메서드
     *
     * 테스트마다 Review 객체를 만드는 코드 중복 제거
     */
    private Review createMockReview(Long reviewId, String content, Float star, Long memberId, String storeName) {
        // Mock Member 생성
        Member mockMember = Member.builder()
                .id(memberId)
                .name("테스트회원")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("test_uid")
                .socialType(SocialType.GOOGLE)
                .email("test@example.com")
                .phoneNumber("01012345678")
                .point(100)
                .build();

        // Mock Store 생성
        Store mockStore = Store.builder()
                .id(1L)
                .name(storeName)
                .detailAddress("서울시 강남구")
                .managerNumber(1012345678L)
                .build();

        // Mock Review 생성
        // Note: @CreatedDate는 실제 JPA에서만 동작하므로 테스트에서는 직접 설정
        // 하지만 Review 빌더에 createdAt setter가 없어서 리플렉션으로 처리할 수도 있지만,
        // 간단하게 @Builder.Default를 사용하거나 현재 시간을 반환하도록 가정
        Review review = Review.builder()
                .id(reviewId)
                .content(content)
                .star(star)
                .user(mockMember)
                .store(mockStore)
                .build();

        // createdAt은 @CreatedDate로 자동 설정되지만, 단위 테스트에서는 JPA Auditing이 작동하지 않으므로
        // 리플렉션을 사용하여 createdAt 필드를 직접 설정
        try {
            java.lang.reflect.Field createdAtField = Review.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(review, LocalDateTime.now());
        } catch (Exception e) {
            // 리플렉션 실패 시 무시 (테스트 환경에서는 큰 문제 없음)
        }

        return review;
    }
}
