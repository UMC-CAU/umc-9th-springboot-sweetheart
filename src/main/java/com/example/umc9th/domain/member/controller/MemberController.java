package com.example.umc9th.domain.member.controller;

import com.example.umc9th.domain.member.dto.MemberRequest;
import com.example.umc9th.domain.member.dto.MemberResponse;
import com.example.umc9th.domain.member.service.MemberService;
import com.example.umc9th.domain.review.dto.ReviewResponse;
import com.example.umc9th.domain.review.service.ReviewQueryService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원 관리 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ReviewQueryService reviewQueryService;

    @Operation(summary = "모든 회원 조회", description = "등록된 모든 회원의 간단한 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<List<MemberResponse.Summary>> getAllMembers() {
        List<MemberResponse.Summary> members = memberService.getAllMembers();
        return ApiResponse.onSuccess(SuccessCode.MEMBER_LIST_OK, members);
    }

    @Operation(summary = "모든 회원 조회 (선호 음식 포함)", description = "등록된 모든 회원의 상세 정보와 선호 음식 목록을 조회합니다.")
    @GetMapping("/with-foods")
    public ApiResponse<List<MemberResponse.Detail>> getAllMembersWithFoods() {
        List<MemberResponse.Detail> members = memberService.getAllMembersWithFoods();
        return ApiResponse.onSuccess(SuccessCode.MEMBER_LIST_OK, members);
    }

    @Operation(summary = "특정 회원 조회", description = "ID로 특정 회원의 기본 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse.Basic> getMemberById(
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long id
    ) {
        MemberResponse.Basic member = memberService.getMemberById(id);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_OK, member);
    }

    @Operation(summary = "특정 회원 조회 (선호 음식 포함)", description = "ID로 회원을 조회하되, 선호 음식 목록도 함께 반환합니다.")
    @GetMapping("/{id}/with-foods")
    public ApiResponse<MemberResponse.Detail> getMemberByIdWithFoods(
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long id
    ) {
        MemberResponse.Detail member = memberService.getMemberByIdWithFoods(id);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_OK, member);
    }

    @Operation(summary = "이름으로 회원 검색", description = "이름으로 회원을 검색합니다.")
    @GetMapping("/search")
    public ApiResponse<List<MemberResponse.Detail>> searchMembersByName(
            @Parameter(description = "회원 이름", example = "홍길동") @RequestParam String name
    ) {
        List<MemberResponse.Detail> members = memberService.searchMembersByName(name);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_LIST_OK, members);
    }

    @Operation(summary = "회원 생성", description = "새로운 회원을 생성합니다.")
    @PostMapping
    public ApiResponse<MemberResponse.Basic> createMember(@Valid @RequestBody MemberRequest.Create request) {
        MemberResponse.Basic member = memberService.createMember(request);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_CREATED, member);
    }

    @Operation(summary = "회원 정보 수정", description = "회원의 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<MemberResponse.Basic> updateMember(
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody MemberRequest.Update request
    ) {
        MemberResponse.Basic member = memberService.updateMember(id, request);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_UPDATED, member);
    }

    @Operation(summary = "회원 삭제", description = "회원을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMember(
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long id
    ) {
        memberService.deleteMember(id);
        return ApiResponse.onSuccess(SuccessCode.MEMBER_DELETED);
    }

    @Operation(
        summary = "특정 회원의 리뷰 조회",
        description = """
            회원이 작성한 리뷰를 조회합니다. (RESTful 자원 중심 설계)
            - 가게별 필터: storeId 파라미터
            - 가게 이름 검색: storeName 파라미터
            - 별점 필터: minScore, maxScore 파라미터
            - 조건 조합 가능
            """
    )
    @GetMapping("/{memberId}/reviews")
    public ApiResponse<List<ReviewResponse.MyReview>> getMemberReviews(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable Long memberId,

            @Parameter(description = "가게 ID", example = "5")
            @RequestParam(required = false) Long storeId,

            @Parameter(description = "가게 이름 (부분 일치)", example = "반이학생")
            @RequestParam(required = false) String storeName,

            @Parameter(description = "최소 별점", example = "4.0")
            @RequestParam(required = false) Float minScore,

            @Parameter(description = "최대 별점", example = "4.99")
            @RequestParam(required = false) Float maxScore
    ) {
        List<ReviewResponse.MyReview> reviews = reviewQueryService.getReviews(
                memberId, storeId, storeName, minScore, maxScore
        );
        return ApiResponse.onSuccess(SuccessCode.OK, reviews);
    }
}
