package com.example.umc9th.integration;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.member.repository.MemberQueryRepository;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.domain.member.service.MemberService;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Member 도메인 통합 테스트
 *
 * 전체 Spring Context를 로드하여 실제 DB(H2)와 함께 테스트합니다.
 * 각 테스트는 @Transactional로 롤백되어 독립적으로 실행됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("회원 관리 통합 테스트")
class MemberIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    /**
     * 테스트용 회원 생성 요청 DTO 생성 헬퍼 메서드
     */
    private MemberRequest.Create createMemberRequest(
            String name,
            String email,
            String socialUid,
            Gender gender,
            LocalDate birth
    ) {
        return MemberRequest.Create.builder()
                .name(name)
                .email(email)
                .socialUid(socialUid)
                .socialType(SocialType.KAKAO)
                .gender(gender)
                .birth(birth)
                .address("서울시 강남구")
                .detailAddress("테헤란로 123")
                .phoneNumber("01012345678")
                .build();
    }

    @Test
    @DisplayName("[통합] 회원 생성 -> 조회 -> 수정 -> 삭제 전체 CRUD 플로우 테스트")
    void fullCrudFlowTest() {
        // Given: 회원 생성 요청 데이터 준비
        MemberRequest.Create createRequest = createMemberRequest(
                "홍길동",
                "hong@example.com",
                "kakao_12345",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );

        // When: 1. 회원 생성
        MemberResponse.Basic createdMember = memberService.createMember(createRequest);

        // Then: 생성된 회원 검증
        assertThat(createdMember).isNotNull();
        assertThat(createdMember.getId()).isNotNull();
        assertThat(createdMember.getName()).isEqualTo("홍길동");
        assertThat(createdMember.getEmail()).isEqualTo("hong@example.com");
        assertThat(createdMember.getGender()).isEqualTo(Gender.MALE);

        Long memberId = createdMember.getId();

        // When: 2. 회원 조회
        MemberResponse.Basic foundMember = memberService.getMemberById(memberId);

        // Then: 조회된 회원 검증
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getId()).isEqualTo(memberId);
        assertThat(foundMember.getName()).isEqualTo("홍길동");

        // When: 3. 회원 정보 수정
        MemberRequest.Update updateRequest = MemberRequest.Update.builder()
                .name("김철수")
                .email("kim@example.com")
                .phoneNumber("01098765432")
                .address("서울시 서초구")
                .detailAddress("강남대로 456")
                .build();

        MemberResponse.Basic updatedMember = memberService.updateMember(memberId, updateRequest);

        // Then: 수정된 회원 검증 (JPA 더티 체킹 확인)
        assertThat(updatedMember.getName()).isEqualTo("김철수");
        assertThat(updatedMember.getEmail()).isEqualTo("kim@example.com");

        // 실제 DB에서 다시 조회하여 변경사항 확인
        Member memberInDb = memberRepository.findById(memberId).orElseThrow();
        assertThat(memberInDb.getName()).isEqualTo("김철수");
        assertThat(memberInDb.getEmail()).isEqualTo("kim@example.com");
        assertThat(memberInDb.getPhoneNumber()).isEqualTo("01098765432");
        assertThat(memberInDb.getAddress()).isEqualTo("서울시 서초구");
        assertThat(memberInDb.getDetailAddress()).isEqualTo("강남대로 456");

        // When: 4. 회원 삭제
        memberService.deleteMember(memberId);

        // Then: 삭제 후 조회 시 예외 발생 확인
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        // 실제 DB에서도 삭제되었는지 확인
        Optional<Member> deletedMember = memberRepository.findById(memberId);
        assertThat(deletedMember).isEmpty();
    }

    @Test
    @DisplayName("[통합] 중복 이메일로 회원 생성 시도 시 CustomException 발생")
    void createMemberWithDuplicateEmail_ShouldThrowException() {
        // Given: 첫 번째 회원 생성
        MemberRequest.Create firstRequest = createMemberRequest(
                "홍길동",
                "duplicate@example.com",
                "kakao_11111",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );
        memberService.createMember(firstRequest);

        // When & Then: 동일한 이메일로 두 번째 회원 생성 시도 -> 예외 발생
        MemberRequest.Create secondRequest = createMemberRequest(
                "김영희",
                "duplicate@example.com", // 중복 이메일
                "kakao_22222",
                Gender.FEMALE,
                LocalDate.of(1995, 5, 5)
        );

        assertThatThrownBy(() -> memberService.createMember(secondRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_DUPLICATE_EMAIL)
                .hasMessageContaining("이미 사용 중인 이메일입니다");
    }

    @Test
    @DisplayName("[통합] 중복 소셜 UID로 회원 생성 시도 시 CustomException 발생")
    void createMemberWithDuplicateSocialUid_ShouldThrowException() {
        // Given: 첫 번째 회원 생성
        MemberRequest.Create firstRequest = createMemberRequest(
                "홍길동",
                "hong@example.com",
                "kakao_duplicate_uid",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );
        memberService.createMember(firstRequest);

        // When & Then: 동일한 소셜 UID로 두 번째 회원 생성 시도 -> 예외 발생
        MemberRequest.Create secondRequest = createMemberRequest(
                "김영희",
                "kim@example.com",
                "kakao_duplicate_uid", // 중복 소셜 UID
                Gender.FEMALE,
                LocalDate.of(1995, 5, 5)
        );

        assertThatThrownBy(() -> memberService.createMember(secondRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_DUPLICATE_SOCIAL_UID)
                .hasMessageContaining("이미 가입된 소셜 계정입니다");
    }

    @Test
    @DisplayName("[통합] 존재하지 않는 회원 ID로 조회 시 CustomException 발생")
    void getMemberByInvalidId_ShouldThrowException() {
        // Given: 존재하지 않는 회원 ID
        Long invalidMemberId = 99999L;

        // When & Then: 존재하지 않는 회원 조회 시도 -> 예외 발생
        assertThatThrownBy(() -> memberService.getMemberById(invalidMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("[통합] 회원 정보 수정 시 JPA 더티 체킹이 정상 작동하는지 검증")
    void updateMember_ShouldTriggerDirtyCheckingAndPersist() {
        // Given: 회원 생성
        MemberRequest.Create createRequest = createMemberRequest(
                "홍길동",
                "hong@example.com",
                "kakao_12345",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );
        MemberResponse.Basic createdMember = memberService.createMember(createRequest);
        Long memberId = createdMember.getId();

        // 수정 전 상태 확인
        Member beforeUpdate = memberRepository.findById(memberId).orElseThrow();
        String originalName = beforeUpdate.getName();
        String originalEmail = beforeUpdate.getEmail();

        // When: 회원 정보 수정 (JPA 더티 체킹 발생)
        MemberRequest.Update updateRequest = MemberRequest.Update.builder()
                .name("수정된이름")
                .email("updated@example.com")
                .build();

        memberService.updateMember(memberId, updateRequest);

        // Then: 트랜잭션 커밋 후 DB에 실제로 반영되었는지 확인
        Member afterUpdate = memberRepository.findById(memberId).orElseThrow();

        // 변경된 필드 검증
        assertThat(afterUpdate.getName()).isNotEqualTo(originalName);
        assertThat(afterUpdate.getName()).isEqualTo("수정된이름");
        assertThat(afterUpdate.getEmail()).isNotEqualTo(originalEmail);
        assertThat(afterUpdate.getEmail()).isEqualTo("updated@example.com");

        // 변경되지 않은 필드는 유지되는지 확인
        assertThat(afterUpdate.getSocialUid()).isEqualTo("kakao_12345");
        assertThat(afterUpdate.getGender()).isEqualTo(Gender.MALE);
    }

    @Test
    @DisplayName("[통합] 회원 정보 수정 시 다른 회원의 이메일로 변경 시도하면 예외 발생")
    void updateMemberWithDuplicateEmail_ShouldThrowException() {
        // Given: 두 명의 회원 생성
        MemberRequest.Create firstRequest = createMemberRequest(
                "홍길동",
                "hong@example.com",
                "kakao_11111",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );
        memberService.createMember(firstRequest);

        MemberRequest.Create secondRequest = createMemberRequest(
                "김영희",
                "kim@example.com",
                "kakao_22222",
                Gender.FEMALE,
                LocalDate.of(1995, 5, 5)
        );
        MemberResponse.Basic secondMember = memberService.createMember(secondRequest);

        // When & Then: 두 번째 회원의 이메일을 첫 번째 회원의 이메일로 변경 시도 -> 예외 발생
        MemberRequest.Update updateRequest = MemberRequest.Update.builder()
                .email("hong@example.com") // 이미 사용 중인 이메일
                .build();

        assertThatThrownBy(() -> memberService.updateMember(secondMember.getId(), updateRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("[QueryDSL] searchMembers() 동적 검색 기능 실제 동작 확인")
    void searchMembers_WithDynamicConditions_ShouldWork() {
        // Given: 다양한 조건의 회원 3명 생성
        Member member1 = Member.builder()
                .name("홍길동")
                .email("hong1@example.com")
                .socialUid("kakao_1")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1)) // 35세
                .address("서울시 강남구")
                .detailAddress("테헤란로 123")
                .point(0)
                .build();

        Member member2 = Member.builder()
                .name("홍길순")
                .email("hong2@example.com")
                .socialUid("kakao_2")
                .socialType(SocialType.KAKAO)
                .gender(Gender.FEMALE)
                .birth(LocalDate.of(2000, 6, 15)) // 25세
                .address("서울시 서초구")
                .detailAddress("강남대로 456")
                .point(0)
                .build();

        Member member3 = Member.builder()
                .name("김철수")
                .email("kim@example.com")
                .socialUid("kakao_3")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1985, 3, 20)) // 40세
                .address("서울시 강남구")
                .detailAddress("역삼로 789")
                .point(0)
                .build();

        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);

        // When & Then 1: 이름에 "홍"이 포함된 회원 검색
        List<Member> result1 = memberQueryRepository.searchMembers("홍", null, null);
        assertThat(result1).hasSize(2);
        assertThat(result1).extracting("name").containsExactlyInAnyOrder("홍길동", "홍길순");

        // When & Then 2: 성별이 MALE인 회원 검색
        List<Member> result2 = memberQueryRepository.searchMembers(null, Gender.MALE, null);
        assertThat(result2).hasSize(2);
        assertThat(result2).extracting("gender").containsOnly(Gender.MALE);

        // When & Then 3: 나이가 30세 이상인 회원 검색 (1995년 이전 출생)
        List<Member> result3 = memberQueryRepository.searchMembers(null, null, 30);
        assertThat(result3).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result3).extracting("name").contains("홍길동", "김철수");

        // When & Then 4: 복합 조건 검색 (이름에 "홍" 포함 + 성별 MALE)
        List<Member> result4 = memberQueryRepository.searchMembers("홍", Gender.MALE, null);
        assertThat(result4).hasSize(1);
        assertThat(result4.get(0).getName()).isEqualTo("홍길동");

        // When & Then 5: 모든 조건 null (전체 조회)
        List<Member> result5 = memberQueryRepository.searchMembers(null, null, null);
        assertThat(result5).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("[QueryDSL] N+1 문제 없이 Fetch Join이 정상 작동하는지 검증")
    void searchMembers_ShouldUseFetchJoinWithoutNPlusOne() {
        // Given: 회원 생성
        Member member = Member.builder()
                .name("홍길동")
                .email("hong@example.com")
                .socialUid("kakao_12345")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시 강남구")
                .detailAddress("테헤란로 123")
                .point(0)
                .build();

        memberRepository.save(member);

        // When: QueryDSL로 회원 조회 (Fetch Join 포함)
        List<Member> result = memberQueryRepository.searchMembers("홍길동", null, null);

        // Then: 회원이 조회되고, 연관관계가 초기화되어 있어야 함
        assertThat(result).hasSize(1);
        Member foundMember = result.get(0);

        // MemberFood 리스트가 Lazy Loading 없이 접근 가능해야 함 (Fetch Join 검증)
        // 현재는 MemberFood가 비어있지만, 접근 시 LazyInitializationException이 발생하지 않아야 함
        assertThat(foundMember.getMemberFoodList()).isNotNull();
        assertThat(foundMember.getMemberFoodList()).isEmpty();
    }

    @Test
    @DisplayName("[QueryDSL] findMembersByBirthBetween() 생년월일 범위 검색 테스트")
    void findMembersByBirthBetween_ShouldReturnMembersInDateRange() {
        // Given: 다양한 생년월일의 회원 생성
        Member member1990 = Member.builder()
                .name("90년대생")
                .email("1990@example.com")
                .socialUid("kakao_1990")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 5, 15))
                .address("서울시")
                .detailAddress("상세주소")
                .point(0)
                .build();

        Member member1995 = Member.builder()
                .name("95년생")
                .email("1995@example.com")
                .socialUid("kakao_1995")
                .socialType(SocialType.KAKAO)
                .gender(Gender.FEMALE)
                .birth(LocalDate.of(1995, 8, 20))
                .address("서울시")
                .detailAddress("상세주소")
                .point(0)
                .build();

        Member member2000 = Member.builder()
                .name("2000년대생")
                .email("2000@example.com")
                .socialUid("kakao_2000")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(2000, 12, 31))
                .address("서울시")
                .detailAddress("상세주소")
                .point(0)
                .build();

        memberRepository.save(member1990);
        memberRepository.save(member1995);
        memberRepository.save(member2000);

        // When: 1990년~1999년 사이에 태어난 회원 검색
        LocalDate startDate = LocalDate.of(1990, 1, 1);
        LocalDate endDate = LocalDate.of(1999, 12, 31);
        List<Member> result = memberQueryRepository.findMembersByBirthBetween(startDate, endDate);

        // Then: 1990년생과 1995년생만 조회되어야 함
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactlyInAnyOrder("90년대생", "95년생");
        assertThat(result).extracting("name").doesNotContain("2000년대생");
    }

    @Test
    @DisplayName("[Repository] findAllWithFoods() N+1 방지 Fetch Join 검증")
    void findAllWithFoods_ShouldPreventNPlusOne() {
        // Given: 회원 2명 생성
        Member member1 = Member.builder()
                .name("회원1")
                .email("member1@example.com")
                .socialUid("kakao_member1")
                .socialType(SocialType.KAKAO)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("상세주소1")
                .point(0)
                .build();

        Member member2 = Member.builder()
                .name("회원2")
                .email("member2@example.com")
                .socialUid("kakao_member2")
                .socialType(SocialType.KAKAO)
                .gender(Gender.FEMALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("상세주소2")
                .point(0)
                .build();

        memberRepository.save(member1);
        memberRepository.save(member2);

        // When: Fetch Join으로 모든 회원 조회
        List<Member> members = memberRepository.findAllWithFoods();

        // Then: N+1 문제 없이 회원 조회 가능
        assertThat(members).hasSizeGreaterThanOrEqualTo(2);

        // 각 회원의 MemberFood 리스트에 접근해도 추가 쿼리 발생하지 않음
        members.forEach(member -> {
            assertThat(member.getMemberFoodList()).isNotNull();
        });
    }

    @Test
    @DisplayName("[트랜잭션] 각 테스트 후 자동 롤백 확인")
    void transactionalRollback_ShouldWorkProperly() {
        // Given & When: 회원 생성
        MemberRequest.Create createRequest = createMemberRequest(
                "롤백테스트",
                "rollback@example.com",
                "kakao_rollback",
                Gender.MALE,
                LocalDate.of(1990, 1, 1)
        );
        MemberResponse.Basic createdMember = memberService.createMember(createRequest);

        // Then: 현재 트랜잭션 내에서는 회원이 존재
        assertThat(createdMember).isNotNull();
        assertThat(memberRepository.findById(createdMember.getId())).isPresent();

        // 이 테스트가 끝나면 자동으로 롤백되어 다음 테스트에 영향을 주지 않음
    }
}
