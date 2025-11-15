package com.example.umc9th.domain.mission.controller;

import com.example.umc9th.domain.mission.dto.MissionRequest;
import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.service.MissionCommandService;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "미션 관리", description = "미션 추가/도전/완료 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionCommandService missionCommandService;

    @Operation(
            summary = "가게에 미션 추가하기",
            description = """
                가게에 새로운 미션을 추가합니다.

                **Validation:**
                - storeId: 가게 ID 필수, DB에 존재해야 함
                - name: 미션 이름 필수, 1자 이상 100자 이하
                - deadline: 미션 마감일 필수, 미래 날짜여야 함
                - conditional: 미션 조건 필수, 5자 이상 200자 이하
                - point: 보상 포인트 필수, 0 이상 100000 이하
                """
    )
    @PostMapping
    public ApiResponse<MissionResponse.CreateMission> createMission(
            @Valid @RequestBody MissionRequest.CreateMissionDTO request
    ) {
        MissionResponse.CreateMission response = missionCommandService.createMission(request);
        return ApiResponse.onSuccess(SuccessCode.MISSION_CREATED, response);
    }

    @Operation(
            summary = "미션 도전하기",
            description = """
                미션을 도전 중인 미션에 추가합니다.

                **Validation:**
                - missionId: 미션 ID 필수, DB에 존재해야 함
                - memberId: 회원 ID 필수, DB에 존재해야 함 (현재는 하드코딩, 추후 인증에서 자동 추출)

                **생성 시 기본 상태:** AVAILABLE (도전 가능 상태)
                """
    )
    @PostMapping("/challenge")
    public ApiResponse<MissionResponse.ChallengeMission> challengeMission(
            @Valid @RequestBody MissionRequest.ChallengeMissionDTO request
    ) {
        MissionResponse.ChallengeMission response = missionCommandService.challengeMission(request);
        return ApiResponse.onSuccess(SuccessCode.MISSION_CREATED, response);
    }
}
