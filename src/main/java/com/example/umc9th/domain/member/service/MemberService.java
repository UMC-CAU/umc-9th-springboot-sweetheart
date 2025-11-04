package com.example.umc9th.domain.member.service;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.entity.Member;
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

        // TODO: 실제 프로젝트에서는 여기에 추가 로직이 필요합니다:
        // 1. 중복 이메일 체크
        // 2. 소셜 UID 중복 체크
        // 3. Food 엔티티 조회 및 MemberFood 매핑

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

        return MemberResponse.Basic.from(savedMember);
    }

    @Transactional
    public MemberResponse.Basic updateMember(Long id, MemberRequest.Update request) {
        log.info("[MemberService.updateMember] 회원 수정 - ID: {}", id);

        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // TODO: Member 엔티티에 update 메서드를 추가하여 사용하는 것이 권장됩니다
        // member.updateInfo(request.getName(), request.getEmail(), ...);

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
