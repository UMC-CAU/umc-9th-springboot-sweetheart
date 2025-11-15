package com.example.umc9th.domain.mission.dto;

import com.example.umc9th.global.validation.annotation.ExistMember;
import com.example.umc9th.global.validation.annotation.ExistMission;
import com.example.umc9th.global.validation.annotation.ExistStore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 미션 관련 요청 DTO 모음
 */
public class MissionRequest {

    /**
     * 가게에 미션 추가 요청 DTO
     */
    @Schema(description = "가게에 미션 추가 요청")
    public record CreateMissionDTO(
            @Schema(description = "가게 ID", example = "1")
            @NotNull(message = "가게 ID는 필수입니다")
            @ExistStore
            Long storeId,

            @Schema(description = "미션 이름", example = "리뷰 3개 작성하기")
            @NotBlank(message = "미션 이름은 필수입니다")
            @Size(min = 1, max = 100, message = "미션 이름은 1자 이상 100자 이하여야 합니다")
            String name,

            @Schema(description = "미션 마감일", example = "2025-12-31")
            @NotNull(message = "미션 마감일은 필수입니다")
            @Future(message = "미션 마감일은 미래 날짜여야 합니다")
            LocalDate deadline,

            @Schema(description = "미션 조건", example = "반이학생 가게에서 리뷰 3개 작성")
            @NotBlank(message = "미션 조건은 필수입니다")
            @Size(min = 5, max = 200, message = "미션 조건은 5자 이상 200자 이하여야 합니다")
            String conditional,

            @Schema(description = "보상 포인트", example = "500")
            @NotNull(message = "보상 포인트는 필수입니다")
            @Min(value = 0, message = "보상 포인트는 0 이상이어야 합니다")
            @Max(value = 100000, message = "보상 포인트는 100000 이하여야 합니다")
            Integer point
    ) {}

    /**
     * 미션 도전하기 요청 DTO
     */
    @Schema(description = "미션 도전하기 요청")
    public record ChallengeMissionDTO(
            @Schema(description = "미션 ID", example = "1")
            @NotNull(message = "미션 ID는 필수입니다")
            @ExistMission
            Long missionId,

            @Schema(description = "회원 ID (현재는 하드코딩, 추후 인증에서 자동 추출)", example = "1")
            @NotNull(message = "회원 ID는 필수입니다")
            @ExistMember
            Long memberId
    ) {}
}
