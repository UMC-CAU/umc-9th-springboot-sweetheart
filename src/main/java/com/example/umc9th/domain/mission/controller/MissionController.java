package com.example.umc9th.domain.mission.controller;

import com.example.umc9th.domain.mission.dto.MissionRequest;
import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.service.MissionCommandService;
import com.example.umc9th.domain.mission.service.MissionQueryService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "미션 관리", description = "미션 추가/도전/완료 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class MissionController {

    private final MissionCommandService missionCommandService;
    private final MissionQueryService missionQueryService;

    @Operation(summary = "특정 가게의 미션 목록 조회", description = "특정 가게의 미션 목록을 조회합니다. 페이징을 포함합니다.")
    @GetMapping("/stores/{storeId}/missions")
    public ApiResponse<MissionResponse.MissionPreViewListDTO> getStoreMissions(
            @Parameter(description = "가게 ID", example = "1") @PathVariable(name = "storeId") Long storeId,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1") @com.example.umc9th.global.validation.annotation.CheckPage @RequestParam(name = "page") Integer page) {
        return ApiResponse.onSuccess(SuccessCode.OK, missionQueryService.getStoreMissions(storeId, page));
    }

    @Operation(summary = "내가 진행중인 미션 목록 조회", description = "내가 진행중인 미션 목록을 조회합니다. 페이징을 포함합니다.")
    @GetMapping("/missions/ongoing")
    public ApiResponse<MissionResponse.MissionPreViewListDTO> getMyOngoingMissions(
            @Parameter(description = "회원 ID (임시)", example = "1") @RequestParam(name = "memberId") Long memberId,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1") @com.example.umc9th.global.validation.annotation.CheckPage @RequestParam(name = "page") Integer page) {
        return ApiResponse.onSuccess(SuccessCode.OK, missionQueryService.getMyOngoingMissions(memberId, page));
    }

    @Operation(summary = "가게에 미션 추가하기", description = """
            가게에 새로운 미션을 추가합니다.

            **Validation:**
            - storeId: 가게 ID 필수, DB에 존재해야 함
            - name: 미션 이름 필수, 1자 이상 100자 이하
            - deadline: 미션 마감일 필수, 미래 날짜여야 함
            - conditional: 미션 조건 필수, 5자 이상 200자 이하
            - point: 보상 포인트 필수, 0 이상 100000 이하
            """)
    @PostMapping("/missions")
    public ApiResponse<MissionResponse.CreateMission> createMission(
            @Valid @RequestBody MissionRequest.CreateMissionDTO request) {
        MissionResponse.CreateMission response = missionCommandService.createMission(request);
        return ApiResponse.onSuccess(SuccessCode.MISSION_CREATED, response);
    }

    @Operation(summary = "미션 도전하기", description = """
            미션을 도전 중인 미션에 추가합니다.

            **Validation:**
            - missionId: 미션 ID 필수, DB에 존재해야 함
            - memberId: 회원 ID 필수, DB에 존재해야 함 (현재는 하드코딩, 추후 인증에서 자동 추출)

            **생성 시 기본 상태:** AVAILABLE (도전 가능 상태)
            """)
    @PostMapping("/missions/challenge")
    public ApiResponse<MissionResponse.ChallengeMission> challengeMission(
            @Valid @RequestBody MissionRequest.ChallengeMissionDTO request) {
        MissionResponse.ChallengeMission response = missionCommandService.challengeMission(request);
        return ApiResponse.onSuccess(SuccessCode.MISSION_CREATED, response);
    }

    @Operation(summary = "진행 중인 미션 완료하기", description = """
            진행 중인 미션을 완료 상태로 변경합니다.

            **Validation:**
            - memberMissionId: 미션 ID 필수, DB에 존재해야 함
            - status: IN_PROGRESS 상태여야 함
            """)
    @PatchMapping("/missions/{memberMissionId}/complete")
    public ApiResponse<MissionResponse.ChallengeMission> completeMission(
            @Parameter(description = "진행 중인 미션 ID", example = "1") @PathVariable(name = "memberMissionId") Long memberMissionId) {
        MissionResponse.ChallengeMission response = missionCommandService.completeMission(memberMissionId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
