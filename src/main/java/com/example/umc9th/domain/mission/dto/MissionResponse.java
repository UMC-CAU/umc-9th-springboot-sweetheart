package com.example.umc9th.domain.mission.dto;

import com.example.umc9th.domain.mission.entity.Mission;
import com.example.umc9th.domain.mission.entity.mapping.MemberMission;
import com.example.umc9th.domain.mission.enums.MissionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 미션 관련 응답 DTO 모음
 */
public class MissionResponse {

    /**
     * 미션 추가 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "미션 추가 응답")
    public static class CreateMission {
        @Schema(description = "생성된 미션 ID", example = "1")
        private Long missionId;

        @Schema(description = "미션 이름", example = "리뷰 3개 작성하기")
        private String name;

        @Schema(description = "미션 마감일", example = "2025-12-31")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate deadline;

        @Schema(description = "미션 생성 시간", example = "2025-01-15T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static CreateMission from(Mission mission) {
            return CreateMission.builder()
                    .missionId(mission.getId())
                    .name(mission.getName())
                    .deadline(mission.getDeadline())
                    .createdAt(mission.getCreatedAt())
                    .build();
        }
    }

    /**
     * 미션 도전하기 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "미션 도전하기 응답")
    public static class ChallengeMission {
        @Schema(description = "생성된 도전 미션 ID", example = "1")
        private Long memberMissionId;

        @Schema(description = "미션 상태", example = "AVAILABLE")
        private MissionStatus status;

        public static ChallengeMission from(MemberMission memberMission) {
            return ChallengeMission.builder()
                    .memberMissionId(memberMission.getId())
                    .status(memberMission.getStatus())
                    .build();
        }
    }

    /**
     * 미션 목록 조회 응답 DTO (페이징 포함 - Page 사용)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "미션 목록 조회 응답 (Page)")
    public static class MissionPreViewListDTO {
        @Schema(description = "미션 목록")
        private java.util.List<MissionPreViewDTO> missionList;

        @Schema(description = "현재 페이지 미션 개수", example = "10")
        private Integer listSize;

        @Schema(description = "전체 페이지 수", example = "5")
        private Integer totalPage;

        @Schema(description = "전체 미션 개수", example = "50")
        private Long totalElements;

        @Schema(description = "첫 페이지 여부", example = "true")
        private Boolean isFirst;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private Boolean isLast;
    }

    /**
     * 미션 목록 조회 응답 DTO (무한 스크롤 - Slice 사용)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "미션 목록 조회 응답 (Slice - 무한 스크롤)")
    public static class MissionPreViewSliceDTO {
        @Schema(description = "미션 목록")
        private java.util.List<MissionPreViewDTO> missionList;

        @Schema(description = "현재 페이지 미션 개수", example = "10")
        private Integer listSize;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;

        @Schema(description = "첫 페이지 여부", example = "true")
        private Boolean isFirst;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private Boolean isLast;
    }

    /**
     * 미션 상세 정보 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MissionPreViewDTO {
        private Long id;
        private String name;
        private String conditional;
        private Integer point;
        private LocalDate deadline;
        private LocalDateTime createdAt;

        public static MissionPreViewDTO from(Mission mission) {
            return MissionPreViewDTO.builder()
                    .id(mission.getId())
                    .name(mission.getName())
                    .conditional(mission.getConditional())
                    .point(mission.getPoint())
                    .deadline(mission.getDeadline())
                    .createdAt(mission.getCreatedAt())
                    .build();
        }
    }
}
