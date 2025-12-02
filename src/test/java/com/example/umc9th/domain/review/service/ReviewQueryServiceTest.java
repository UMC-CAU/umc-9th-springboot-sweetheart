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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

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
         *        - 실제 ReviewQueryRepository가 아닌 Mock 객체
         *        - given()으로 동작 방식을 정의해줘야 함
         */
        @Mock
        private ReviewQueryRepository reviewQueryRepository;

        /**
         * @InjectMocks: @Mock 객체들을 자동 주입
         *               - ReviewQueryService의 생성자에 reviewQueryRepository를 자동 주입
         *               - 실제로 테스트할 대상 객체
         */
        @InjectMocks
        private ReviewQueryService reviewQueryService;

        // ===== getMyReviewsList() 테스트 (Page 방식) =====

        /**
         * getMyReviewsList() - 정상 조회 (Page 방식)
         *
         * Page 방식의 특징:
         * - COUNT 쿼리 실행으로 전체 페이지 수, 전체 데이터 개수 제공
         * - 페이지 번호 UI (1, 2, 3...)에 적합
         */
        @Test
        @DisplayName("[Page] 내가 작성한 리뷰 목록을 페이징으로 조회할 수 있다")
        void getMyReviewsList_Success() {
                // Given: Repository가 Page 객체를 반환
                Long memberId = 1L;
                Integer page = 1; // 1-based (사용자 입력)

                List<Review> mockReviews = Arrays.asList(
                                createMockReview(1L, "맛있어요", 5.0f, memberId, "반이학생"),
                                createMockReview(2L, "좋아요", 4.0f, memberId, "맘스터치"));

                PageRequest pageRequest = PageRequest.of(0, 10); // 0-based (내부 처리)
                Page<Review> mockPage = new PageImpl<>(mockReviews, pageRequest, 15); // 전체 15개

                // ReviewQueryRepository.findReviewsWithPaging() 호출 시 mockPage 반환
                given(reviewQueryRepository.findReviewsWithPaging(
                                eq(memberId), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                                .willReturn(mockPage);

                // When: Service의 getMyReviewsList() 호출
                ReviewResponse.ReviewPreViewListDTO result = reviewQueryService.getMyReviewsList(memberId, page);

                // Then: 결과 검증
                assertThat(result).isNotNull();
                assertThat(result.getReviewList()).hasSize(2);
                assertThat(result.getTotalPage()).isEqualTo(2); // 15개 / 10개 = 2페이지
                assertThat(result.getTotalElements()).isEqualTo(15L);
                assertThat(result.getIsFirst()).isTrue(); // 첫 페이지
                assertThat(result.getIsLast()).isFalse(); // 마지막 아님

                // Repository 메서드 호출 확인
                then(reviewQueryRepository).should(times(1))
                                .findReviewsWithPaging(eq(memberId), isNull(), isNull(), isNull(), isNull(),
                                                any(PageRequest.class));
        }

        @Test
        @DisplayName("[Page] 빈 결과도 정상 처리된다")
        void getMyReviewsList_EmptyResult() {
                // Given: 빈 Page
                Long memberId = 1L;
                Integer page = 1;

                PageRequest pageRequest = PageRequest.of(0, 10);
                Page<Review> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

                given(reviewQueryRepository.findReviewsWithPaging(
                                eq(memberId), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                                .willReturn(emptyPage);

                // When
                ReviewResponse.ReviewPreViewListDTO result = reviewQueryService.getMyReviewsList(memberId, page);

                // Then: 빈 리스트지만 에러 없이 반환
                assertThat(result.getReviewList()).isEmpty();
                assertThat(result.getTotalElements()).isEqualTo(0L);
                assertThat(result.getTotalPage()).isEqualTo(0);
        }

        // ===== getMyReviewsListWithSlice() 테스트 (Slice 방식) =====

        /**
         * getMyReviewsListWithSlice() - 정상 조회 (Slice 방식)
         *
         * Slice 방식의 특징:
         * - COUNT 쿼리를 실행하지 않음 (성능 우수)
         * - 다음 페이지 존재 여부(hasNext)만 제공
         * - 무한 스크롤 UI에 적합
         */
        @Test
        @DisplayName("[Slice] 내가 작성한 리뷰 목록을 무한 스크롤 방식으로 조회할 수 있다")
        void getMyReviewsListWithSlice_Success() {
                // Given: Repository가 Slice 객체를 반환
                Long memberId = 1L;
                Integer page = 1; // 1-based

                List<Review> mockReviews = Arrays.asList(
                                createMockReview(1L, "리뷰1", 5.0f, memberId, "가게1"),
                                createMockReview(2L, "리뷰2", 4.5f, memberId, "가게2"),
                                createMockReview(3L, "리뷰3", 4.0f, memberId, "가게3"));

                PageRequest pageRequest = PageRequest.of(0, 10); // 0-based
                Slice<Review> mockSlice = new SliceImpl<>(mockReviews, pageRequest, true); // hasNext = true

                given(reviewQueryRepository.findReviewsWithSlice(
                                eq(memberId), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                                .willReturn(mockSlice);

                // When
                ReviewResponse.ReviewPreViewSliceDTO result = reviewQueryService.getMyReviewsListWithSlice(memberId,
                                page);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getReviewList()).hasSize(3);
                assertThat(result.getHasNext()).isTrue(); // 다음 페이지 있음
                assertThat(result.getIsFirst()).isTrue(); // 첫 페이지
                assertThat(result.getIsLast()).isFalse(); // 마지막 아님

                then(reviewQueryRepository).should(times(1))
                                .findReviewsWithSlice(eq(memberId), isNull(), isNull(), isNull(), isNull(),
                                                any(PageRequest.class));
        }

        @Test
        @DisplayName("[Slice] 마지막 페이지에서는 hasNext가 false다")
        void getMyReviewsListWithSlice_LastPage() {
                // Given: 마지막 페이지 (hasNext = false)
                Long memberId = 1L;
                Integer page = 2;

                List<Review> mockReviews = List.of(
                                createMockReview(4L, "리뷰4", 3.5f, memberId, "가게4"));

                PageRequest pageRequest = PageRequest.of(1, 10); // 2페이지 (0-based로 1)
                Slice<Review> mockSlice = new SliceImpl<>(mockReviews, pageRequest, false); // hasNext = false

                given(reviewQueryRepository.findReviewsWithSlice(
                                eq(memberId), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                                .willReturn(mockSlice);

                // When
                ReviewResponse.ReviewPreViewSliceDTO result = reviewQueryService.getMyReviewsListWithSlice(memberId,
                                page);

                // Then
                assertThat(result.getHasNext()).isFalse(); // 다음 페이지 없음
                assertThat(result.getIsLast()).isTrue(); // 마지막 페이지
        }

        // ===== getReviews() 통합 조회 테스트 =====

        /**
         * getReviews() - 모든 필터 조건 적용
         *
         * 범용 API 메서드 테스트
         */
        @Test
        @DisplayName("[통합] 모든 필터 조건을 적용하여 리뷰를 조회할 수 있다")
        void getReviews_AllFilters_Success() {
                // Given: 모든 필터 조건 설정
                Long memberId = 1L;
                Long storeId = 10L;
                String storeName = "맛집";
                Float minScore = 3.0f;
                Float maxScore = 5.0f;

                List<Review> mockReviews = Arrays.asList(
                                createMockReview(1L, "완벽해요", 5.0f, memberId, "맛집A"),
                                createMockReview(2L, "좋아요", 4.0f, memberId, "맛집B"));

                given(reviewQueryRepository.findReviews(memberId, storeId, storeName, minScore, maxScore))
                                .willReturn(mockReviews);

                // When
                List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(memberId, storeId, storeName,
                                minScore, maxScore);

                // Then
                assertThat(result).hasSize(2);
                then(reviewQueryRepository).should().findReviews(memberId, storeId, storeName, minScore, maxScore);
        }

        /**
         * getReviews() - 일부 필터만 적용
         */
        @Test
        @DisplayName("[통합] 일부 필터만 적용할 수 있다")
        void getReviews_PartialFilters_Success() {
                // Given: memberId와 minScore만 설정 (storeId, storeName, maxScore는 null)
                Long memberId = 1L;
                Float minScore = 4.0f;

                List<Review> mockReviews = List.of(
                                createMockReview(1L, "좋아요", 4.5f, memberId, "가게1"));

                given(reviewQueryRepository.findReviews(memberId, null, null, minScore, null))
                                .willReturn(mockReviews);

                // When
                List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(memberId, null, null, minScore,
                                null);

                // Then
                assertThat(result).hasSize(1);
                then(reviewQueryRepository).should().findReviews(memberId, null, null, minScore, null);
        }

        /**
         * getReviews() - 필터 없이 전체 조회
         */
        @Test
        @DisplayName("[통합] 필터 없이 전체 리뷰를 조회할 수 있다")
        void getReviews_NoFilters_Success() {
                // Given: 모든 필터가 null
                List<Review> mockReviews = Arrays.asList(
                                createMockReview(1L, "리뷰1", 5.0f, 1L, "가게1"),
                                createMockReview(2L, "리뷰2", 3.0f, 2L, "가게2"),
                                createMockReview(3L, "리뷰3", 4.0f, 3L, "가게3"));

                given(reviewQueryRepository.findReviews(null, null, null, null, null))
                                .willReturn(mockReviews);

                // When
                List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(null, null, null, null, null);

                // Then: 모든 리뷰 반환
                assertThat(result).hasSize(3);
        }

        /**
         * getReviews() - 빈 결과 처리
         */
        @Test
        @DisplayName("[통합] 조건에 맞는 리뷰가 없으면 빈 리스트를 반환한다")
        void getReviews_EmptyResult() {
                // Given: Repository가 빈 리스트 반환
                given(reviewQueryRepository.findReviews(any(), any(), any(), any(), any()))
                                .willReturn(Collections.emptyList());

                // When
                List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(1L, null, "존재하지않는가게", 4.0f, 5.0f);

                // Then: 빈 리스트여야 함 (예외 발생 X)
                assertThat(result).isEmpty();
        }

        // ===== 별점 범위 검증 테스트 (중요!) =====

        /**
         * getReviews() - 최소 별점이 0보다 작으면 예외 발생
         *
         * 별점 검증 테스트는 매우 중요!
         * - validateScoreRange()는 private 메서드이지만
         * - public 메서드(getReviews)를 통해 간접 테스트
         */
        @Test
        @DisplayName("[검증] 최소 별점이 0보다 작으면 IllegalArgumentException이 발생한다")
        void getReviews_MinScoreLessThanZero_ThrowsException() {
                // Given: minScore < 0
                Float invalidMinScore = -1.0f;
                Float maxScore = 5.0f;

                // When & Then: IllegalArgumentException 발생 확인
                assertThatThrownBy(() -> reviewQueryService.getReviews(1L, null, null, invalidMinScore, maxScore))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("최소 별점은 0.0 이상 5.0 이하여야 합니다");

                // Repository는 호출되지 않아야 함 (검증 단계에서 실패했으므로)
                then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
        }

        /**
         * getReviews() - 최소 별점이 5보다 크면 예외 발생
         */
        @Test
        @DisplayName("[검증] 최소 별점이 5보다 크면 IllegalArgumentException이 발생한다")
        void getReviews_MinScoreGreaterThanFive_ThrowsException() {
                // Given: minScore > 5
                Float invalidMinScore = 6.0f;

                // When & Then
                assertThatThrownBy(() -> reviewQueryService.getReviews(1L, null, null, invalidMinScore, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("최소 별점은 0.0 이상 5.0 이하여야 합니다");

                then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
        }

        /**
         * getReviews() - 최대 별점이 0보다 작으면 예외 발생
         */
        @Test
        @DisplayName("[검증] 최대 별점이 0보다 작으면 IllegalArgumentException이 발생한다")
        void getReviews_MaxScoreLessThanZero_ThrowsException() {
                // Given: maxScore < 0
                Float invalidMaxScore = -0.5f;

                // When & Then
                assertThatThrownBy(() -> reviewQueryService.getReviews(1L, null, null, null, invalidMaxScore))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("최대 별점은 0.0 이상 5.0 이하여야 합니다");

                then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
        }

        /**
         * getReviews() - 최대 별점이 5보다 크면 예외 발생
         */
        @Test
        @DisplayName("[검증] 최대 별점이 5보다 크면 IllegalArgumentException이 발생한다")
        void getReviews_MaxScoreGreaterThanFive_ThrowsException() {
                // Given: maxScore > 5
                Float minScore = 3.0f;
                Float invalidMaxScore = 7.0f;

                // When & Then
                assertThatThrownBy(() -> reviewQueryService.getReviews(1L, null, null, minScore, invalidMaxScore))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("최대 별점은 0.0 이상 5.0 이하여야 합니다");

                then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
        }

        /**
         * getReviews() - 최소 별점이 최대 별점보다 크면 예외 발생
         *
         * 가장 중요한 검증 로직!
         */
        @Test
        @DisplayName("[검증] 최소 별점이 최대 별점보다 크면 IllegalArgumentException이 발생한다")
        void getReviews_MinScoreGreaterThanMaxScore_ThrowsException() {
                // Given: minScore > maxScore (논리적으로 불가능한 범위)
                Float minScore = 4.5f;
                Float maxScore = 2.0f;

                // When & Then
                assertThatThrownBy(() -> reviewQueryService.getReviews(1L, null, null, minScore, maxScore))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("최소 별점은 최대 별점보다 클 수 없습니다");

                then(reviewQueryRepository).should(never()).findReviews(any(), any(), any(), any(), any());
        }

        /**
         * getReviews() - 유효한 경계값 테스트 (0.0 ~ 5.0)
         */
        @Test
        @DisplayName("[검증] 별점 범위가 0.0 ~ 5.0 사이면 정상 조회된다")
        void getReviews_ValidScoreRange_Success() {
                // Given: 유효한 경계값 (0.0 ~ 5.0)
                Long memberId = 1L;
                Float minScore = 0.0f; // 최소 경계값
                Float maxScore = 5.0f; // 최대 경계값

                given(reviewQueryRepository.findReviews(memberId, null, null, minScore, maxScore))
                                .willReturn(Collections.emptyList());

                // When: 예외 없이 실행되어야 함
                assertThatCode(() -> reviewQueryService.getReviews(memberId, null, null, minScore, maxScore))
                                .doesNotThrowAnyException();

                // Then
                then(reviewQueryRepository).should().findReviews(memberId, null, null, minScore, maxScore);
        }

        // ===== DTO 변환 검증 테스트 =====

        /**
         * DTO 변환이 올바르게 수행되는지 검증
         *
         * Service는 엔티티를 DTO로 변환하는 책임도 가지므로
         * 변환 로직이 제대로 동작하는지 확인 필요
         */
        @Test
        @DisplayName("[DTO] Review 엔티티가 ReviewResponse.MyReview DTO로 올바르게 변환된다")
        void entityToDtoConversion_Success() {
                // Given
                Long memberId = 1L;
                Review mockReview = createMockReview(1L, "변환 테스트", 4.5f, memberId, "테스트가게");

                given(reviewQueryRepository.findReviews(eq(memberId), any(), any(), any(), any()))
                                .willReturn(List.of(mockReview));

                // When
                List<ReviewResponse.MyReview> result = reviewQueryService.getReviews(memberId, null, null, null, null);

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
