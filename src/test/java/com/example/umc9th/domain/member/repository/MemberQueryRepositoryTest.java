package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.entity.mapping.MemberFood;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.member.enums.Gender;
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
 * MemberQueryRepository 테스트
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
 * @Import(JpaAuditingConfig.class):
 * - JPA Auditing 활성화 (createdAt, updatedAt 자동 설정)
 * - Member 엔티티가 BaseEntity를 상속받는 경우 필요
 *
 * 테스트 목적:
 * - QueryDSL 동적 쿼리 검증
 * - 이름/성별/나이 필터링 동작 확인
 * - 음식 선호도로 회원 검색 동작 확인
 * - 생년월일 범위 검색 동작 확인
 * - 주소별 회원 수 조회 동작 확인
 * - N+1 문제 방지 (Fetch Join) 검증
 * - 나이 계산 로직 검증 (경계값 테스트 포함)
 */
@DataJpaTest
@Import({QueryDslConfig.class, MemberQueryRepository.class, JpaAuditingConfig.class})
@DisplayName("MemberQueryRepository 테스트")
class MemberQueryRepositoryTest {

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 데이터 참조 변수
    private Food korean, chinese, japanese, western;
    private Member member1, member2, member3, member4, member5, member6, member7, member8, member9, member10;

    @BeforeEach
    void setUp() {
        // Food 데이터 생성
        korean = Food.builder().name(FoodName.KOREAN).build();
        chinese = Food.builder().name(FoodName.CHINESE).build();
        japanese = Food.builder().name(FoodName.JAPANESE).build();
        western = Food.builder().name(FoodName.WESTERN).build();

        entityManager.persist(korean);
        entityManager.persist(chinese);
        entityManager.persist(japanese);
        entityManager.persist(western);

        // Member 데이터 생성 (다양한 나이, 성별, 주소)
        createTestMembers();

        // MemberFood 매핑 데이터 생성
        createMemberFoodMappings();

        // 쓰기 지연 SQL 실행 및 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();
    }

    private void createTestMembers() {
        // 현재 날짜: 2025년 11월 15일 기준
        // 나이 계산: 현재 년도 - 생년월일 년도 + 1

        // 1. 김철수 - 남성, 25세 (2001년 1월 1일생 - 경계값: 1월생)
        member1 = createMember("김철수", Gender.MALE, LocalDate.of(2001, 1, 1),
                "서울시 강남구", "테헤란로 123", "kakao_1001");

        // 2. 이영희 - 여성, 30세 (1996년 6월 15일생)
        member2 = createMember("이영희", Gender.FEMALE, LocalDate.of(1996, 6, 15),
                "서울시 서초구", "서초대로 456", "naver_2001");

        // 3. 박민수 - 남성, 22세 (2004년 3월 20일생)
        member3 = createMember("박민수", Gender.MALE, LocalDate.of(2004, 3, 20),
                "서울시 강남구", "역삼로 789", "google_3001");

        // 4. 최지은 - 여성, 28세 (1998년 9월 10일생)
        member4 = createMember("최지은", Gender.FEMALE, LocalDate.of(1998, 9, 10),
                "경기도 성남시", "판교역로 111", "kakao_4001");

        // 5. 정민호 - 남성, 35세 (1991년 12월 31일생 - 경계값: 12월생)
        member5 = createMember("정민호", Gender.MALE, LocalDate.of(1991, 12, 31),
                "서울시 종로구", "종로 222", "apple_5001");

        // 6. 강수연 - 여성, 26세 (2000년 2월 14일생)
        member6 = createMember("강수연", Gender.FEMALE, LocalDate.of(2000, 2, 14),
                "서울시 강남구", "강남대로 333", "naver_6001");

        // 7. 윤태영 - 남성, 40세 (1986년 7월 7일생)
        member7 = createMember("윤태영", Gender.MALE, LocalDate.of(1986, 7, 7),
                "경기도 성남시", "분당로 444", "google_7001");

        // 8. 임수진 - 여성, 23세 (2003년 11월 11일생)
        member8 = createMember("임수진", Gender.FEMALE, LocalDate.of(2003, 11, 11),
                "서울시 서초구", "서초중앙로 555", "kakao_8001");

        // 9. 김민재 - 남성, 29세 (1997년 4월 4일생)
        member9 = createMember("김민재", Gender.MALE, LocalDate.of(1997, 4, 4),
                "서울시 강남구", "테헤란로 666", "naver_9001");

        // 10. 박수현 - NONE, 32세 (1994년 5월 5일생)
        member10 = createMember("박수현", Gender.NONE, LocalDate.of(1994, 5, 5),
                "경기도 용인시", "수지로 777", "apple_10001");
    }

    private Member createMember(String name, Gender gender, LocalDate birth,
                                String address, String detailAddress, String socialUid) {
        Member member = Member.builder()
                .name(name)
                .gender(gender)
                .birth(birth)
                .address(address)
                .detailAddress(detailAddress)
                .socialUid(socialUid)
                .socialType(SocialType.KAKAO)
                .point(0)
                .email(socialUid + "@test.com")
                .phoneNumber("010-0000-0000")
                .build();
        entityManager.persist(member);
        return member;
    }

    private void createMemberFoodMappings() {
        // Member 1 (김철수) - 한식, 중식 선호
        persistMemberFood(member1, korean);
        persistMemberFood(member1, chinese);

        // Member 2 (이영희) - 일식 선호
        persistMemberFood(member2, japanese);

        // Member 3 (박민수) - 한식, 양식 선호
        persistMemberFood(member3, korean);
        persistMemberFood(member3, western);

        // Member 4 (최지은) - 중식 선호
        persistMemberFood(member4, chinese);

        // Member 5 (정민호) - 한식 선호
        persistMemberFood(member5, korean);

        // Member 6 (강수연) - 양식 선호
        persistMemberFood(member6, western);

        // Member 7 (윤태영) - 일식, 양식 선호
        persistMemberFood(member7, japanese);
        persistMemberFood(member7, western);

        // Member 8 (임수진) - 한식 선호
        persistMemberFood(member8, korean);

        // Member 9 (김민재) - 중식, 일식 선호
        persistMemberFood(member9, chinese);
        persistMemberFood(member9, japanese);

        // Member 10 (박수현) - 양식 선호
        persistMemberFood(member10, western);
    }

    private void persistMemberFood(Member member, Food food) {
        MemberFood memberFood = MemberFood.builder()
                .member(member)
                .food(food)
                .build();
        entityManager.persist(memberFood);
    }

    // ========== searchMembers() 테스트 ==========

    @Test
    @DisplayName("전체 회원 조회 - 필터 없음")
    void searchMembers_NoFilter_ReturnsAllMembers() {
        // Given: 필터 없음
        String name = null;
        Gender gender = null;
        Integer minAge = null;

        // When: 전체 조회
        List<Member> results = memberQueryRepository.searchMembers(name, gender, minAge);

        // Then: 모든 회원이 조회됨 (10명)
        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("이름으로 검색 - 부분 일치 (김씨 성을 가진 회원)")
    void searchMembers_ByName_ReturnsMatchingMembers() {
        // Given: "김" 검색 (김철수, 김민재)
        String name = "김";

        // When
        List<Member> results = memberQueryRepository.searchMembers(name, null, null);

        // Then: "김"이 포함된 회원 2명 조회
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("김철수", "김민재");
    }

    @Test
    @DisplayName("성별 필터링 - 남성만 조회")
    void searchMembers_ByGender_ReturnsOnlyMale() {
        // Given: 남성 필터
        Gender gender = Gender.MALE;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, gender, null);

        // Then: 남성 회원만 조회 (김철수, 박민수, 정민호, 윤태영, 김민재 = 5명)
        assertThat(results).hasSize(5);
        assertThat(results)
                .extracting(Member::getGender)
                .containsOnly(Gender.MALE);
    }

    @Test
    @DisplayName("성별 필터링 - 여성만 조회")
    void searchMembers_ByGender_ReturnsOnlyFemale() {
        // Given: 여성 필터
        Gender gender = Gender.FEMALE;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, gender, null);

        // Then: 여성 회원만 조회 (이영희, 최지은, 강수연, 임수진 = 4명)
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getGender)
                .containsOnly(Gender.FEMALE);
    }

    @Test
    @DisplayName("나이 필터링 - 30세 이상 회원 조회 (나이 계산 로직 검증)")
    void searchMembers_ByMinAge_Returns30AndAbove() {
        // Given: 30세 이상 필터
        // 현재 년도: 2025년 기준
        // 나이 계산: 현재 년도 - 생년월일 년도 + 1
        // 30세 이상 = 1996년 이하 출생
        // 대상: 이영희(30세), 정민호(35세), 윤태영(40세), 박수현(32세) = 4명
        Integer minAge = 30;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, null, minAge);

        // Then: 30세 이상 회원만 조회
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("이영희", "정민호", "윤태영", "박수현");

        // 나이 계산 로직 검증: 각 회원의 나이 확인
        int currentYear = LocalDate.now().getYear();
        results.forEach(member -> {
            int age = currentYear - member.getBirth().getYear() + 1;
            assertThat(age).isGreaterThanOrEqualTo(minAge);
        });
    }

    @Test
    @DisplayName("나이 필터링 - 25세 이상 회원 조회 (경계값 테스트: 1월생)")
    void searchMembers_ByMinAge_Returns25AndAbove_EdgeCase_January() {
        // Given: 25세 이상 필터
        // 2025년 기준 25세 = 2001년 이하 출생
        // 김철수: 2001년 1월 1일생 (25세) - 경계값: 1월생 포함 확인
        Integer minAge = 25;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, null, minAge);

        // Then: 25세 이상 회원 조회 (7명)
        // 김철수(25), 이영희(30), 최지은(28), 정민호(35), 강수연(26), 윤태영(40), 김민재(29), 박수현(32)
        assertThat(results).hasSize(8);

        // 김철수(1월 1일생) 포함 확인
        assertThat(results)
                .extracting(Member::getName)
                .contains("김철수");
    }

    @Test
    @DisplayName("나이 필터링 - 35세 이상 회원 조회 (경계값 테스트: 12월생)")
    void searchMembers_ByMinAge_Returns35AndAbove_EdgeCase_December() {
        // Given: 35세 이상 필터
        // 2025년 기준 35세 = 1991년 이하 출생
        // 정민호: 1991년 12월 31일생 (35세) - 경계값: 12월생 포함 확인
        Integer minAge = 35;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, null, minAge);

        // Then: 35세 이상 회원 조회 (정민호, 윤태영 = 2명)
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("정민호", "윤태영");

        // 정민호(12월 31일생) 포함 확인
        assertThat(results)
                .extracting(Member::getName)
                .contains("정민호");
    }

    @Test
    @DisplayName("나이 필터링 - 22세 회원 조회 (최소 나이 경계값)")
    void searchMembers_ByMinAge_Returns22_MinimumAge() {
        // Given: 22세 이상 필터
        // 2025년 기준 22세 = 2004년 이하 출생
        // 박민수: 2004년 3월 20일생 (22세)
        // 모든 테스트 데이터가 22세 이상이므로 10명 모두 조회됨
        Integer minAge = 22;

        // When
        List<Member> results = memberQueryRepository.searchMembers(null, null, minAge);

        // Then: 22세 이상 회원 모두 조회 (10명 전부)
        assertThat(results).hasSize(10);

        // 박민수(22세, 최소 나이) 포함 확인
        assertThat(results)
                .extracting(Member::getName)
                .contains("박민수");
    }

    @Test
    @DisplayName("복합 조건 검색 - 이름 + 성별 + 나이")
    void searchMembers_CombinedFilters_ReturnsMatchingMembers() {
        // Given: "김" + 남성 + 25세 이상
        String name = "김";
        Gender gender = Gender.MALE;
        Integer minAge = 25;

        // When
        List<Member> results = memberQueryRepository.searchMembers(name, gender, minAge);

        // Then: 조건에 맞는 회원만 조회 (김철수: 남성 25세, 김민재: 남성 29세 = 2명)
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("김철수", "김민재");

        // 모든 결과가 남성이고, 이름에 "김"이 포함됨
        assertThat(results)
                .allMatch(member -> member.getGender() == Gender.MALE)
                .allMatch(member -> member.getName().contains("김"));
    }

    @Test
    @DisplayName("검색 결과 없음 - 존재하지 않는 이름")
    void searchMembers_NoMatch_ReturnsEmptyList() {
        // Given: 존재하지 않는 이름
        String name = "홍길동";

        // When
        List<Member> results = memberQueryRepository.searchMembers(name, null, null);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("N+1 문제 방지 - MemberFood와 Food가 Fetch Join으로 조회됨")
    void searchMembers_FetchJoin_LoadsMemberFoodAndFood() {
        // Given: 강남구 거주 회원 검색 (이름에 "김" 포함)
        String name = "김";

        // When
        List<Member> results = memberQueryRepository.searchMembers(name, null, null);

        // Then: LazyInitializationException 발생 안 함
        assertThat(results).isNotEmpty();

        // MemberFood와 Food 접근 시 추가 쿼리 없이 조회 가능
        results.forEach(member -> {
            assertThat(member.getMemberFoodList()).isNotNull();
            member.getMemberFoodList().forEach(memberFood -> {
                assertThat(memberFood.getFood()).isNotNull();
                assertThat(memberFood.getFood().getName()).isNotNull();
            });
        });
    }

    // ========== findMembersByFoodId() 테스트 ==========

    @Test
    @DisplayName("음식 선호도로 회원 검색 - 한식을 선호하는 회원 조회")
    void findMembersByFoodId_Korean_ReturnsMatchingMembers() {
        // Given: 한식 ID
        Long koreanId = korean.getId();

        // When
        List<Member> results = memberQueryRepository.findMembersByFoodId(koreanId);

        // Then: 한식을 선호하는 회원 조회 (김철수, 박민수, 정민호, 임수진 = 4명)
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("김철수", "박민수", "정민호", "임수진");
    }

    @Test
    @DisplayName("음식 선호도로 회원 검색 - 양식을 선호하는 회원 조회")
    void findMembersByFoodId_Western_ReturnsMatchingMembers() {
        // Given: 양식 ID
        Long westernId = western.getId();

        // When
        List<Member> results = memberQueryRepository.findMembersByFoodId(westernId);

        // Then: 양식을 선호하는 회원 조회 (박민수, 강수연, 윤태영, 박수현 = 4명)
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("박민수", "강수연", "윤태영", "박수현");
    }

    @Test
    @DisplayName("음식 선호도로 회원 검색 - 일식을 선호하는 회원 조회")
    void findMembersByFoodId_Japanese_ReturnsMatchingMembers() {
        // Given: 일식 ID
        Long japaneseId = japanese.getId();

        // When
        List<Member> results = memberQueryRepository.findMembersByFoodId(japaneseId);

        // Then: 일식을 선호하는 회원 조회 (이영희, 윤태영, 김민재 = 3명)
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("이영희", "윤태영", "김민재");
    }

    @Test
    @DisplayName("음식 선호도로 회원 검색 - 존재하지 않는 음식 ID")
    void findMembersByFoodId_InvalidId_ReturnsEmptyList() {
        // Given: 존재하지 않는 음식 ID
        Long invalidId = 999L;

        // When
        List<Member> results = memberQueryRepository.findMembersByFoodId(invalidId);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    // ========== findMembersByBirthBetween() 테스트 ==========

    @Test
    @DisplayName("생년월일 범위 검색 - 1995년~2000년 사이 출생 회원 조회")
    void findMembersByBirthBetween_1995To2000_ReturnsMatchingMembers() {
        // Given: 1995-01-01 ~ 2000-12-31
        LocalDate startDate = LocalDate.of(1995, 1, 1);
        LocalDate endDate = LocalDate.of(2000, 12, 31);

        // When
        List<Member> results = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 해당 기간 출생 회원 조회 (이영희, 최지은, 김민재, 강수연 = 4명)
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("이영희", "최지은", "김민재", "강수연");

        // 생년월일 오름차순 정렬 검증
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getBirth())
                    .isBeforeOrEqualTo(results.get(i + 1).getBirth());
        }
    }

    @Test
    @DisplayName("생년월일 범위 검색 - 2000년 이후 출생 회원 조회")
    void findMembersByBirthBetween_After2000_ReturnsMatchingMembers() {
        // Given: 2000-01-01 ~ 2010-12-31
        LocalDate startDate = LocalDate.of(2000, 1, 1);
        LocalDate endDate = LocalDate.of(2010, 12, 31);

        // When
        List<Member> results = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 2000년 이후 출생 회원 조회 (김철수, 박민수, 강수연, 임수진 = 4명)
        assertThat(results).hasSize(4);
        assertThat(results)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("김철수", "박민수", "강수연", "임수진");
    }

    @Test
    @DisplayName("생년월일 범위 검색 - 경계값 테스트 (1월 1일 ~ 12월 31일)")
    void findMembersByBirthBetween_EdgeCases_IncludesBoundaries() {
        // Given: 2001-01-01 ~ 1991-12-31 (김철수 ~ 정민호)
        // 김철수: 2001년 1월 1일 (경계값: 시작일)
        // 정민호: 1991년 12월 31일 (경계값: 종료일)
        LocalDate startDate = LocalDate.of(1991, 12, 31);
        LocalDate endDate = LocalDate.of(2001, 1, 1);

        // When
        List<Member> results = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 경계값 포함 확인
        assertThat(results)
                .extracting(Member::getName)
                .contains("김철수", "정민호");
    }

    @Test
    @DisplayName("생년월일 범위 검색 - 결과 없음 (미래 날짜)")
    void findMembersByBirthBetween_FutureDate_ReturnsEmptyList() {
        // Given: 미래 날짜 범위
        LocalDate startDate = LocalDate.of(2030, 1, 1);
        LocalDate endDate = LocalDate.of(2040, 12, 31);

        // When
        List<Member> results = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 빈 리스트 반환
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("생년월일 범위 검색 - 생년월일 오름차순 정렬 검증")
    void findMembersByBirthBetween_OrderByBirthAsc_VerifySorting() {
        // Given: 전체 범위
        LocalDate startDate = LocalDate.of(1980, 1, 1);
        LocalDate endDate = LocalDate.of(2010, 12, 31);

        // When
        List<Member> results = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 생년월일 오름차순 정렬 확인
        assertThat(results).hasSize(10);

        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getBirth())
                    .isBeforeOrEqualTo(results.get(i + 1).getBirth());
        }

        // 첫 번째 회원: 윤태영 (1986년 7월 7일)
        assertThat(results.get(0).getName()).isEqualTo("윤태영");

        // 마지막 회원: 박민수 (2004년 3월 20일)
        assertThat(results.get(results.size() - 1).getName()).isEqualTo("박민수");
    }

    // ========== countMembersByAddress() 테스트 ==========

    @Test
    @DisplayName("주소별 회원 수 조회 - 서울시 거주 회원 수")
    void countMembersByAddress_Seoul_ReturnsCount() {
        // Given: "서울시" 주소
        String address = "서울시";

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 서울시 거주 회원 7명 (김철수, 이영희, 박민수, 정민호, 강수연, 임수진, 김민재)
        assertThat(count).isEqualTo(7);
    }

    @Test
    @DisplayName("주소별 회원 수 조회 - 강남구 거주 회원 수")
    void countMembersByAddress_Gangnam_ReturnsCount() {
        // Given: "강남구" 주소
        String address = "강남구";

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 강남구 거주 회원 4명 (김철수, 박민수, 강수연, 김민재)
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("주소별 회원 수 조회 - 경기도 거주 회원 수")
    void countMembersByAddress_Gyeonggi_ReturnsCount() {
        // Given: "경기도" 주소
        String address = "경기도";

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 경기도 거주 회원 3명 (최지은, 윤태영, 박수현)
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("주소별 회원 수 조회 - 부분 일치 검색 (성남시)")
    void countMembersByAddress_PartialMatch_ReturnsCount() {
        // Given: "성남시" 주소
        String address = "성남시";

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 성남시 거주 회원 2명 (최지은, 윤태영)
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("주소별 회원 수 조회 - 존재하지 않는 주소")
    void countMembersByAddress_NoMatch_ReturnsZero() {
        // Given: 존재하지 않는 주소
        String address = "제주도";

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 0 반환
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("주소별 회원 수 조회 - null 주소는 모든 회원 수 반환")
    void countMembersByAddress_Null_ReturnsAllCount() {
        // Given: null 주소
        String address = null;

        // When
        Long count = memberQueryRepository.countMembersByAddress(address);

        // Then: 전체 회원 수 반환 (10명)
        assertThat(count).isEqualTo(10);
    }
}
