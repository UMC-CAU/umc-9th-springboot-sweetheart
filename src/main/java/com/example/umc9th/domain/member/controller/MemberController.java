package com.example.umc9th.domain.member.controller;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원 관리 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @Operation(summary = "모든 회원 조회", description = "등록된 모든 회원 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "모든 회원 조회 (N+1 해결)", description = "Entity Graph를 사용하여 회원과 선호 음식을 한 번에 조회합니다.")
    @GetMapping("/with-foods")
    public ResponseEntity<List<Member>> getAllMembersWithFoods() {
        List<Member> members = memberRepository.findAllWithFoods();
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "특정 회원 조회", description = "ID로 특정 회원을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(
            @Parameter(description = "회원 ID", example = "1")
            @PathVariable Long id
    ) {
        return memberRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "특정 회원 조회 (N+1 해결)", description = "ID로 회원을 조회하되, 선호 음식도 함께 가져옵니다.")
    @GetMapping("/{id}/with-foods")
    public ResponseEntity<Member> getMemberByIdWithFoods(
            @Parameter(description = "회원 ID", example = "1")
            @PathVariable Long id
    ) {
        return memberRepository.findByIdWithFoods(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "이름으로 회원 검색", description = "이름으로 회원을 검색합니다 (선호 음식 포함).")
    @GetMapping("/search")
    public ResponseEntity<List<Member>> searchMembersByName(
            @Parameter(description = "회원 이름", example = "홍길동")
            @RequestParam String name
    ) {
        List<Member> members = memberRepository.findByNameWithFoods(name);
        return ResponseEntity.ok(members);
    }
}
