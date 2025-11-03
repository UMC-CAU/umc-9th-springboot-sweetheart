package com.example.umc9th.domain.member.service;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * MemberService 테스트
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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {

    /**
     * @Mock: 가짜 객체 생성
     * - 실제 MemberRepository가 아닌 Mock 객체
     * - given()으로 동작 방식을 정의해줘야 함
     */
    @Mock
    private MemberRepository memberRepository;

    /**
     * @InjectMocks: @Mock 객체들을 자동 주입
     * - MemberService의 생성자에 memberRepository를 자동 주입
     * - 실제로 테스트할 대상 객체
     */
    @InjectMocks
    private MemberService memberService;


    // ===== 회원 조회 테스트 =====

    /**
     * 회원 조회 성공 테스트
     *
     * BDD 스타일 (Behavior-Driven Development):
     * - given(): "이런 상황이 주어졌을 때"
     * - when(): "이걸 실행하면"
     * - then(): "이런 결과가 나와야 한다"
     */
    @Test
    @DisplayName("ID로 회원을 조회할 수 있다")
    void getMemberById_Success() {
        // Given: Repository가 회원을 반환한다고 가정
        Long memberId = 1L;
        Member mockMember = createMockMember(memberId, "홍길동", "hong@example.com");

        // Mockito: "findById(1L)이 호출되면 mockMember를 반환해라"
        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(mockMember));

        // When: Service의 getMemberById() 호출
        MemberResponse.Basic result = memberService.getMemberById(memberId);

        // Then: 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(memberId);
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getEmail()).isEqualTo("hong@example.com");

        // 검증: Repository의 findById가 1번 호출되었는지
        then(memberRepository).should(times(1)).findById(memberId);
    }

    /**
     * 회원 조회 실패 테스트 (존재하지 않는 회원)
     *
     * 예외 처리 테스트는 매우 중요!
     * - 비즈니스 로직의 대부분은 예외 처리
     */
    @Test
    @DisplayName("존재하지 않는 회원 ID로 조회하면 CustomException이 발생한다")
    void getMemberById_NotFound() {
        // Given: Repository가 빈 Optional 반환
        Long nonExistentId = 999L;
        given(memberRepository.findById(nonExistentId))
                .willReturn(Optional.empty());

        // When & Then: CustomException 발생 확인
        assertThatThrownBy(() -> memberService.getMemberById(nonExistentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        // 검증: Repository 호출되었는지
        then(memberRepository).should().findById(nonExistentId);
    }


    // ===== 회원 목록 조회 테스트 =====

    /**
     * 전체 회원 목록 조회 테스트
     */
    @Test
    @DisplayName("모든 회원 목록을 조회할 수 있다")
    void getAllMembers_Success() {
        // Given: Repository가 회원 리스트 반환
        List<Member> mockMembers = Arrays.asList(
                createMockMember(1L, "회원1", "member1@example.com"),
                createMockMember(2L, "회원2", "member2@example.com"),
                createMockMember(3L, "회원3", "member3@example.com")
        );

        given(memberRepository.findAll())
                .willReturn(mockMembers);

        // When: Service의 getAllMembers() 호출
        List<MemberResponse.Summary> result = memberService.getAllMembers();

        // Then: 결과 검증
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting("name")
                .containsExactly("회원1", "회원2", "회원3");

        then(memberRepository).should().findAll();
    }

    /**
     * 빈 목록 조회 테스트
     */
    @Test
    @DisplayName("회원이 없으면 빈 리스트를 반환한다")
    void getAllMembers_EmptyList() {
        // Given: 빈 리스트 반환
        given(memberRepository.findAll())
                .willReturn(Arrays.asList());

        // When
        List<MemberResponse.Summary> result = memberService.getAllMembers();

        // Then: 빈 리스트여야 함 (예외 발생 X)
        assertThat(result).isEmpty();
    }


    // ===== 회원 생성 테스트 =====

    /**
     * 회원 생성 성공 테스트
     */
    @Test
    @DisplayName("새로운 회원을 생성할 수 있다")
    void createMember_Success() {
        // Given: 요청 DTO 준비
        MemberRequest.Create request = MemberRequest.Create.builder()
                .name("신규회원")
                .gender(Gender.MALE)
                .birth(LocalDate.of(1995, 5, 5))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("google_new")
                .socialType(SocialType.GOOGLE)
                .email("new@example.com")
                .phoneNumber("01099999999")
                .build();

        // Repository의 save() 동작 정의
        // any(Member.class): "어떤 Member 객체가 오든"
        given(memberRepository.save(any(Member.class)))
                .willAnswer(invocation -> {
                    // save()에 전달된 Member 객체를 가져와서
                    Member member = invocation.getArgument(0);
                    // ID를 부여한 채로 반환 (실제 DB처럼)
                    return Member.builder()
                            .id(1L)  // ID 자동 생성 시뮬레이션
                            .name(member.getName())
                            .gender(member.getGender())
                            .birth(member.getBirth())
                            .address(member.getAddress())
                            .detailAddress(member.getDetailAddress())
                            .socialUid(member.getSocialUid())
                            .socialType(member.getSocialType())
                            .email(member.getEmail())
                            .phoneNumber(member.getPhoneNumber())
                            .point(member.getPoint())
                            .build();
                });

        // When: 회원 생성
        MemberResponse.Basic result = memberService.createMember(request);

        // Then: 검증
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("신규회원");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPoint()).isEqualTo(0);  // 신규 회원 포인트는 0

        // save() 호출 확인
        then(memberRepository).should().save(any(Member.class));
    }


    // ===== 회원 삭제 테스트 =====

    /**
     * 회원 삭제 성공 테스트
     */
    @Test
    @DisplayName("회원을 삭제할 수 있다")
    void deleteMember_Success() {
        // Given: 삭제할 회원이 존재
        Long memberId = 1L;
        Member mockMember = createMockMember(memberId, "삭제될회원", "delete@example.com");

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(mockMember));

        // delete()는 void 메서드이므로 willDoNothing() 사용
        willDoNothing().given(memberRepository).delete(mockMember);

        // When: 삭제 실행
        assertThatCode(() -> memberService.deleteMember(memberId))
                .doesNotThrowAnyException();

        // Then: 메서드 호출 확인
        then(memberRepository).should().findById(memberId);
        then(memberRepository).should().delete(mockMember);
    }

    /**
     * 존재하지 않는 회원 삭제 시도 테스트
     */
    @Test
    @DisplayName("존재하지 않는 회원을 삭제하려고 하면 CustomException이 발생한다")
    void deleteMember_NotFound() {
        // Given: 회원이 존재하지 않음
        Long nonExistentId = 999L;
        given(memberRepository.findById(nonExistentId))
                .willReturn(Optional.empty());

        // When & Then: 예외 발생
        assertThatThrownBy(() -> memberService.deleteMember(nonExistentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        // delete()는 호출되지 않아야 함
        then(memberRepository).should(never()).delete(any(Member.class));
    }


    // ===== 이름으로 검색 테스트 =====

    /**
     * 이름으로 회원 검색 테스트
     */
    @Test
    @DisplayName("이름으로 회원을 검색할 수 있다")
    void searchMembersByName_Success() {
        // Given: 같은 이름의 회원 여러 명
        String searchName = "홍길동";
        List<Member> mockMembers = Arrays.asList(
                createMockMember(1L, "홍길동", "hong1@example.com"),
                createMockMember(2L, "홍길동", "hong2@example.com")
        );

        given(memberRepository.findByNameWithFoods(searchName))
                .willReturn(mockMembers);

        // When: 검색 실행
        List<MemberResponse.Detail> result = memberService.searchMembersByName(searchName);

        // Then: 2명 조회
        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(member -> member.getName().equals("홍길동"));

        then(memberRepository).should().findByNameWithFoods(searchName);
    }


    // ===== 헬퍼 메서드 =====

    /**
     * Mock Member 생성 헬퍼 메서드
     *
     * 테스트마다 Member 객체를 만드는 코드 중복 제거
     */
    private Member createMockMember(Long id, String name, String email) {
        return Member.builder()
                .id(id)
                .name(name)
                .gender(Gender.MALE)
                .birth(LocalDate.of(1990, 1, 1))
                .address("서울시")
                .detailAddress("강남구")
                .socialUid("uid_" + id)
                .socialType(SocialType.GOOGLE)
                .email(email)
                .phoneNumber("01012345678")
                .point(100)
                .build();
    }
}
