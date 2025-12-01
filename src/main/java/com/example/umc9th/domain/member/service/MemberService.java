package com.example.umc9th.domain.member.service;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.entity.mapping.MemberFood;
import com.example.umc9th.domain.member.repository.FoodRepository;
import com.example.umc9th.domain.member.repository.MemberFoodRepository;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
