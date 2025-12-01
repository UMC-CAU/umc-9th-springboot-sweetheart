package com.example.umc9th.domain.mission.converter;

import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.entity.Mission;
import org.springframework.data.domain.Page;

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
}
