package com.example.umc9th.domain.mission.converter;

import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

public class MissionConverter {

  public static MissionResponse.MissionPreViewListDTO toMissionPreViewListDTO(Page<Mission> missionPage) {
    List<MissionResponse.MissionPreViewDTO> missionList = missionPage.stream()
        .map(MissionResponse.MissionPreViewDTO::from)
        .collect(Collectors.toList());

    return MissionResponse.MissionPreViewListDTO.builder()
        .missionList(missionList)
        .listSize(missionList.size())
        .totalPage(missionPage.getTotalPages())
        .totalElements(missionPage.getTotalElements())
        .isFirst(missionPage.isFirst())
        .isLast(missionPage.isLast())
        .build();
  }

  public static MissionResponse.MissionPreViewListDTO toMemberMissionPreViewListDTO(
      Page<com.example.umc9th.domain.mission.entity.mapping.MemberMission> memberMissionPage) {
    List<MissionResponse.MissionPreViewDTO> missionList = memberMissionPage.stream()
        .map(memberMission -> MissionResponse.MissionPreViewDTO.from(memberMission.getMission()))
        .collect(Collectors.toList());

    return MissionResponse.MissionPreViewListDTO.builder()
        .missionList(missionList)
        .listSize(missionList.size())
        .totalPage(memberMissionPage.getTotalPages())
        .totalElements(memberMissionPage.getTotalElements())
        .isFirst(memberMissionPage.isFirst())
        .isLast(memberMissionPage.isLast())
        .build();
  }

  /**
   * Slice를 MissionPreViewSliceDTO로 변환
   * 다음 페이지 존재 여부만 포함 (COUNT 쿼리 불필요)
   */
  public static MissionResponse.MissionPreViewSliceDTO toMissionPreViewSliceDTO(Slice<Mission> missionSlice) {
    List<MissionResponse.MissionPreViewDTO> missionList = missionSlice.stream()
        .map(MissionResponse.MissionPreViewDTO::from)
        .collect(Collectors.toList());

    return MissionResponse.MissionPreViewSliceDTO.builder()
        .missionList(missionList)
        .listSize(missionList.size())
        .hasNext(missionSlice.hasNext())
        .isFirst(missionSlice.isFirst())
        .isLast(missionSlice.isLast())
        .build();
  }

  /**
   * Slice를 MemberMissionPreViewSliceDTO로 변환
   */
  public static MissionResponse.MissionPreViewSliceDTO toMemberMissionPreViewSliceDTO(
      Slice<com.example.umc9th.domain.mission.entity.mapping.MemberMission> memberMissionSlice) {
    List<MissionResponse.MissionPreViewDTO> missionList = memberMissionSlice.stream()
        .map(memberMission -> MissionResponse.MissionPreViewDTO.from(memberMission.getMission()))
        .collect(Collectors.toList());

    return MissionResponse.MissionPreViewSliceDTO.builder()
        .missionList(missionList)
        .listSize(missionList.size())
        .hasNext(memberMissionSlice.hasNext())
        .isFirst(memberMissionSlice.isFirst())
        .isLast(memberMissionSlice.isLast())
        .build();
  }
}
