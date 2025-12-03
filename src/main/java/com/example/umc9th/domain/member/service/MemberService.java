package com.example.umc9th.domain.member.service;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.entity.mapping.MemberFood;
import com.example.umc9th.domain.member.repository.FoodRepository;
import com.example.umc9th.domain.member.repository.MemberFoodRepository;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.auth.CustomUserDetails;
import com.example.umc9th.global.auth.enums.Role;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.auth.jwt.JwtUtil;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final FoodRepository foodRepository;
    private final MemberFoodRepository memberFoodRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final com.example.umc9th.global.auth.service.AuthService authService;

    /**
     * 일반 로그인을 위한 회원가입
     * - 비밀번호는 BCrypt로 암호화하여 저장
     * - 기본 권한은 ROLE_USER
     */
    @Transactional
    public MemberResponse.Join signup(MemberRequest.Join request) {
        log.info("[MemberService.signup] 회원가입 시작 - email: {}", request.getEmail());

        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            log.warn("[MemberService.signup] 중복된 이메일 - email: {}", request.getEmail());
            throw new CustomException(ErrorCode.MEMBER_DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화 (BCrypt)
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        log.info("[MemberService.signup] 비밀번호 암호화 완료");

        // 3. Member 엔티티 생성 (일반 로그인용)
        Member member = Member.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .role(Role.ROLE_USER)  // 기본 권한: 일반 사용자
                .gender(request.getGender())
                .birth(request.getBirth())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .socialUid("LOCAL_" + request.getEmail())  // 일반 로그인 구분용
                .socialType(SocialType.GOOGLE)  // 임시값 (또는 LOCAL 타입 추가)
                .point(0)
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("[MemberService.signup] 회원가입 완료 - ID: {}, email: {}",
                savedMember.getId(), savedMember.getEmail());

        // 4. 선호 음식 설정
        if (request.getFoodPreferences() != null && !request.getFoodPreferences().isEmpty()) {
            log.info("[MemberService.signup] 선호 음식 매핑 시작 - count: {}",
                    request.getFoodPreferences().size());

            List<Food> foods = foodRepository.findByNameIn(request.getFoodPreferences());
            List<MemberFood> memberFoods = foods.stream()
                    .map(food -> MemberFood.builder()
                            .member(savedMember)
                            .food(food)
                            .build())
                    .collect(Collectors.toList());

            memberFoodRepository.saveAll(memberFoods);
            log.info("[MemberService.signup] 선호 음식 매핑 완료");
        }

        return MemberResponse.Join.from(savedMember);
    }

    /**
     * 로그인 (JWT 토큰 발급)
     * - 이메일/비밀번호 검증
     * - JWT Access Token 발급
     */
    public MemberResponse.Login login(MemberRequest.Login request) {
        log.info("[MemberService.login] 로그인 시도 - email: {}", request.getEmail());

        // 1. 이메일로 회원 조회
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[MemberService.login] 존재하지 않는 이메일 - email: {}", request.getEmail());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            log.warn("[MemberService.login] 비밀번호 불일치 - email: {}", request.getEmail());
            throw new CustomException(ErrorCode.MEMBER_INVALID_PASSWORD);
        }

        // 3. JWT Access Token + Refresh Token 발급
        CustomUserDetails userDetails = new CustomUserDetails(member);
        String accessToken = jwtUtil.createAccessToken(userDetails);
        String refreshToken = jwtUtil.createRefreshToken(userDetails);

        // 4. Refresh Token DB에 저장
        authService.saveRefreshToken(member, refreshToken);

        log.info("[MemberService.login] 로그인 성공 - email: {}, memberId: {}",
                request.getEmail(), member.getId());

        return MemberResponse.Login.of(member, accessToken, refreshToken);
    }

    public List<MemberResponse.Summary> getAllMembers() {
        log.info("[MemberService.getAllMembers] 모든 회원 조회");
        List<Member> members = memberRepository.findAll();
        return members.stream()
                .map(MemberResponse.Summary::from)
                .collect(Collectors.toList());
    }

    public List<MemberResponse.Detail> getAllMembersWithFoods() {
        log.info("[MemberService.getAllMembersWithFoods] 선호 음식 포함 회원 조회");
        List<Member> members = memberRepository.findAllWithFoods();
        return members.stream()
                .map(MemberResponse.Detail::from)
                .collect(Collectors.toList());
    }

    public MemberResponse.Basic getMemberById(Long id) {
        log.info("[MemberService.getMemberById] 회원 조회 - ID: {}", id);
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.Basic.from(member);
    }

    public MemberResponse.Detail getMemberByIdWithFoods(Long id) {
        log.info("[MemberService.getMemberByIdWithFoods] 회원 상세 조회 - ID: {}", id);
        Member member = memberRepository.findByIdWithFoods(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.Detail.from(member);
    }

    public List<MemberResponse.Detail> searchMembersByName(String name) {
        log.info("[MemberService.searchMembersByName] 회원 이름 검색 - name: {}", name);
        List<Member> members = memberRepository.findByNameWithFoods(name);
        return members.stream()
                .map(MemberResponse.Detail::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse.Basic createMember(MemberRequest.Create request) {
        log.info("[MemberService.createMember] 회원 생성 - name: {}, email: {}",
                request.getName(), request.getEmail());

        // 1. 중복 이메일 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            log.warn("[MemberService.createMember] 중복된 이메일 - email: {}", request.getEmail());
            throw new CustomException(ErrorCode.MEMBER_DUPLICATE_EMAIL);
        }

        // 2. 소셜 UID 중복 체크
        if (memberRepository.existsBySocialUid(request.getSocialUid())) {
            log.warn("[MemberService.createMember] 중복된 소셜 UID - socialUid: {}", request.getSocialUid());
            throw new CustomException(ErrorCode.MEMBER_DUPLICATE_SOCIAL_UID);
        }

        // 3. Member 엔티티 생성 및 저장
        Member member = Member.builder()
                .name(request.getName())
                .gender(request.getGender())
                .birth(request.getBirth())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .socialUid(request.getSocialUid())
                .socialType(request.getSocialType())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .point(0)
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("[MemberService.createMember] 회원 생성 완료 - ID: {}", savedMember.getId());

        // 4. Food 엔티티 조회 및 MemberFood 매핑 (선호 음식 설정)
        if (request.getFoodPreferences() != null && !request.getFoodPreferences().isEmpty()) {
            log.info("[MemberService.createMember] 선호 음식 매핑 시작 - count: {}", request.getFoodPreferences().size());

            // FoodName 리스트로 Food 엔티티 조회
            List<Food> foods = foodRepository.findByNameIn(request.getFoodPreferences());

            // MemberFood 매핑 엔티티 생성
            List<MemberFood> memberFoods = foods.stream()
                    .map(food -> MemberFood.builder()
                            .member(savedMember)
                            .food(food)
                            .build())
                    .collect(Collectors.toList());

            // MemberFood 저장
            memberFoodRepository.saveAll(memberFoods);
            log.info("[MemberService.createMember] 선호 음식 매핑 완료 - count: {}", memberFoods.size());
        }

        return MemberResponse.Basic.from(savedMember);
    }

    @Transactional
    public MemberResponse.Basic updateMember(Long id, MemberRequest.Update request) {
        log.info("[MemberService.updateMember] 회원 수정 - ID: {}", id);

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 이메일 변경 시 중복 체크 (다른 회원이 사용 중인지 확인)
        if (request.getEmail() != null && !request.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                log.warn("[MemberService.updateMember] 중복된 이메일 - email: {}", request.getEmail());
                throw new CustomException(ErrorCode.MEMBER_DUPLICATE_EMAIL);
            }
        }

        // JPA 더티 체킹을 활용한 업데이트 (엔티티의 값을 변경하면 자동으로 UPDATE 쿼리 실행)
        member.updateInfo(
                request.getName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getAddress(),
                request.getDetailAddress()
        );

        log.info("[MemberService.updateMember] 회원 수정 완료 - ID: {}", id);

        return MemberResponse.Basic.from(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        log.info("[MemberService.deleteMember] 회원 삭제 - ID: {}", id);

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository.delete(member);

        log.info("[MemberService.deleteMember] 회원 삭제 완료 - ID: {}", id);
    }
}
