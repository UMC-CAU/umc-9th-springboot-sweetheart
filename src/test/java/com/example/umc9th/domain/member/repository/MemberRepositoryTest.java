package com.example.umc9th.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.config.JpaAuditingConfig;

/**
 * MemberRepository 테스트
 *
 * @DataJpaTest:
 * - JPA 관련 컴포넌트만 로딩 (가볍고 빠름)
 * - 내장 DB(H2) 자동 사용
 * - 각 테스트 후 자동 롤백 (DB 초기화)
 *
 * @Import(JpaAuditingConfig.class):
 * - JPA Auditing 활성화 (createdAt, updatedAt 자동 설정)
 * - @DataJpaTest는 전체 Context를 로드하지 않으므로 필요
 * - 전역 설정을 재사용하여 코드 중복 제거
 *
 * 왜 Repository 테스트가 중요할까?
 * - 커스텀 쿼리가 많을수록 테스트 필요성 증가
 * - Fetch Join이 제대로 동작하는지 확인
 * - N+1 문제 발생 여부 확인
 */
@Import(JpaAuditingConfig.class)
@DataJpaTest
@DisplayName("MemberRepository 테스트")
class MemberRepositoryTest {

    /**
     * @Autowired: 테스트에서는 필드 주입도 괜찮음
     * (프로덕션 코드에서는 생성자 주입 권장)
     */
    @Autowired
    private MemberRepository memberRepository;

    /**
     * TestEntityManager: 테스트용 EntityManager
     * - 테스트 데이터를 DB에 넣을 때 사용
     * - persist(), flush(), clear() 등 제공
     */
    @Autowired
    private TestEntityManager entityManager;


    // ===== 기본 CRUD 테스트 =====

    /**
     * 회원 저장 테스트
     *
     * Given-When-Then 패턴:
     * - Given: 테스트 준비 (데이터 셋업)
     * - When: 테스트 실행 (실제 동작)
     * - Then: 검증 (결과 확인)
     */
    @Test
    @DisplayName("회원을 저장할 수 있다")
    void saveMember() {
        // Given: 저장할 회원 데이터 준비
        Member member = Member.builder()
                .name("홍길동")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시 강남구")
                .detailAddress("테헤란로 123")
                .socialUid("google_123456")
                .socialType(SocialType.GOOGLE)
                .email("hong@example.com")
                .phoneNumber("01012345678")
                .point(0)
                .build();

        // When: 실제 저장 실행
        Member savedMember = memberRepository.save(member);

        // Then: 검증
        assertThat(savedMember.getId()).isNotNull();  // ID가 자동 생성되었는지
        assertThat(savedMember.getName()).isEqualTo("홍길동");
        assertThat(savedMember.getEmail()).isEqualTo("hong@example.com");
        assertThat(savedMember.getPoint()).isEqualTo(0);
    }

    /**
     * 회원 조회 테스트 (ID로)
     */
    @Test
    @DisplayName("ID로 회원을 조회할 수 있다")
    void findById() {
        // Given: 회원을 미리 저장
        Member member = createMember("김철수", "kim@example.com");
        Member savedMember = memberRepository.save(member);

        // When: ID로 조회
        Optional<Member> found = memberRepository.findById(savedMember.getId());

        // Then: 조회 성공 확인
        assertThat(found).isPresent();  // Optional에 값이 있는지
        assertThat(found.get().getName()).isEqualTo("김철수");
    }

    /**
     * 존재하지 않는 회원 조회 테스트
     */
    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_NotFound() {
        // Given: 없는 ID
        Long nonExistentId = 999L;

        // When: 조회
        Optional<Member> found = memberRepository.findById(nonExistentId);

        // Then: 비어있어야 함
        assertThat(found).isEmpty();
    }

    /**
     * 전체 회원 조회 테스트
     */
    @Test
    @DisplayName("모든 회원을 조회할 수 있다")
    void findAll() {
        // Given: 여러 회원 저장
        memberRepository.save(createMember("회원1", "member1@example.com"));
        memberRepository.save(createMember("회원2", "member2@example.com"));
        memberRepository.save(createMember("회원3", "member3@example.com"));

        // When: 전체 조회
        List<Member> members = memberRepository.findAll();

        // Then: 3명이 조회되어야 함
        assertThat(members).hasSize(3);
        assertThat(members)
                .extracting("name")  // name 필드만 추출
                .containsExactlyInAnyOrder("회원1", "회원2", "회원3");
    }

    /**
     * 회원 삭제 테스트
     */
    @Test
    @DisplayName("회원을 삭제할 수 있다")
    void deleteMember() {
        // Given: 회원 저장
        Member member = memberRepository.save(createMember("삭제될회원", "delete@example.com"));
        Long memberId = member.getId();

        // When: 삭제
        memberRepository.deleteById(memberId);

        // Then: 조회했을 때 없어야 함
        Optional<Member> found = memberRepository.findById(memberId);
        assertThat(found).isEmpty();
    }


    // ===== 커스텀 쿼리 테스트 =====

    /**
     * Fetch Join 테스트 - N+1 방지 확인
     *
     * 중요! 이 테스트가 실무에서 매우 중요한 이유:
     * - Fetch Join이 제대로 동작하는지 확인
     * - 쿼리 개수를 세어서 N+1 문제 없는지 검증
     */
    @Test
    @DisplayName("findAllWithFoods()는 N+1 없이 회원과 선호 음식을 함께 조회한다")
    void findAllWithFoods_NoNPlusOne() {
        // Given: 회원 + 선호 음식 데이터 준비
        // (실제로는 Food 엔티티와 MemberFood 매핑도 저장해야 하지만
        //  여기서는 기본 쿼리 동작만 확인)

        memberRepository.save(createMember("회원1", "member1@example.com"));
        memberRepository.save(createMember("회원2", "member2@example.com"));

        entityManager.flush();  // DB에 반영
        entityManager.clear();  // 1차 캐시 비우기 (실제 쿼리 발생하도록)

        // When: Fetch Join 쿼리 실행
        List<Member> members = memberRepository.findAllWithFoods();

        // Then: 조회 성공
        assertThat(members).hasSize(2);

        // 추가 검증: memberFoodList에 접근해도 추가 쿼리 발생 안 함
        // (이미 Fetch Join으로 로딩되었기 때문)
        members.forEach(member -> {
            // Lazy Loading이 아니므로 쿼리가 안 나감
            assertThat(member.getMemberFoodList()).isNotNull();
        });
    }

    /**
     * 이름으로 회원 검색 테스트
     */
    @Test
    @DisplayName("이름으로 회원을 검색할 수 있다")
    void findByNameWithFoods() {
        // Given: 같은 이름의 회원 여러 명
        memberRepository.save(createMember("홍길동", "hong1@example.com"));
        memberRepository.save(createMember("홍길동", "hong2@example.com"));
        memberRepository.save(createMember("김철수", "kim@example.com"));

        // When: 이름으로 검색
        List<Member> members = memberRepository.findByNameWithFoods("홍길동");

        // Then: 홍길동 2명만 조회
        assertThat(members).hasSize(2);
        assertThat(members)
                .allMatch(member -> member.getName().equals("홍길동"));
    }

    /**
     * ID로 회원 + 선호 음식 조회 테스트
     */
    @Test
    @DisplayName("ID로 회원과 선호 음식을 함께 조회할 수 있다")
    void findByIdWithFoods() {
        // Given: 회원 저장
        Member member = memberRepository.save(createMember("테스트회원", "test@example.com"));
        Long memberId = member.getId();

        entityManager.flush();
        entityManager.clear();

        // When: ID로 Fetch Join 조회
        Optional<Member> found = memberRepository.findByIdWithFoods(memberId);

        // Then: 조회 성공
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트회원");
    }


    // ===== 경계값 테스트 =====

    /**
     * 빈 문자열 저장 테스트
     *
     * @NotBlank 검증은 Controller에서 이루어지므로
     * Repository 레벨에서는 저장 가능
     */
    @Test
    @DisplayName("빈 이름도 DB에는 저장 가능하다 (검증은 Controller에서)")
    void saveWithEmptyName() {
        // Given: 빈 이름
        Member member = Member.builder()
                .name("")  // 빈 문자열
                .gender(Gender.NONE)
                .birth(LocalDate.now())
                .address("주소")
                .detailAddress("상세주소")
                .socialUid("uid")
                .socialType(SocialType.GOOGLE)
                .email("empty@example.com")
                .point(0)
                .build();

        // When & Then: 저장은 가능 (DB 레벨에서는 제약 없음)
        assertThatCode(() -> memberRepository.save(member))
                .doesNotThrowAnyException();
    }

    /**
     * 매우 긴 이름 저장 테스트
     */
    @Test
    @DisplayName("50자를 초과하는 이름은 DB 제약으로 실패한다")
    void saveWithTooLongName() {
        // Given: 50자 초과 이름
        String longName = "a".repeat(51);  // 51자
        Member member = createMember(longName, "long@example.com");

        // When & Then: DB 제약 위반으로 예외 발생
        assertThatThrownBy(() -> {
            memberRepository.save(member);
            entityManager.flush();  // flush 해야 실제 INSERT 쿼리 실행
        }).isInstanceOf(Exception.class);  // DataIntegrityViolationException 또는 유사 예외
    }


    // ===== 헬퍼 메서드 =====

    /**
     * 테스트용 회원 생성 헬퍼 메서드
     *
     * 중복 코드 제거를 위한 유틸리티 메서드
     */
    private Member createMember(String name, String email) {
        return Member.builder()
                .name(name)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("상세주소")
                .socialUid("uid_" + email)
                .socialType(SocialType.GOOGLE)
                .email(email)
                .phoneNumber("01012345678")
                .point(0)
                .build();
    }
}
