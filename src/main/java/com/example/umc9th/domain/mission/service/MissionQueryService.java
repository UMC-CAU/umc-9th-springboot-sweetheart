package com.example.umc9th.domain.mission.service;

import com.example.umc9th.domain.mission.converter.MissionConverter;
import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.entity.Mission;
import com.example.umc9th.domain.mission.repository.MissionRepository;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.global.response.code.ErrorCode;
import com.example.umc9th.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionQueryService {

  private final MissionRepository missionRepository;
  private final StoreRepository storeRepository;
  private final com.example.umc9th.domain.member.repository.MemberRepository memberRepository;
  private final com.example.umc9th.domain.mission.repository.MemberMissionRepository memberMissionRepository;

  public MissionResponse.MissionPreViewListDTO getStoreMissions(Long storeId, Integer page) {
    Store store = storeRepository.findById(storeId)
        .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

    PageRequest pageRequest = PageRequest.of(page - 1, 10);
    Page<Mission> missionPage = missionRepository.findAllByStore(store, pageRequest);

    return MissionConverter.toMissionPreViewListDTO(missionPage);
  }

  public MissionResponse.MissionPreViewListDTO getMyOngoingMissions(Long memberId, Integer page) {
    com.example.umc9th.domain.member.entity.Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    PageRequest pageRequest = PageRequest.of(page - 1, 10);
    Page<com.example.umc9th.domain.mission.entity.mapping.MemberMission> memberMissionPage = memberMissionRepository
        .findAllByMemberAndStatus(member, com.example.umc9th.domain.mission.enums.MissionStatus.IN_PROGRESS,
            pageRequest);

    return MissionConverter.toMemberMissionPreViewListDTO(memberMissionPage);
  }
}
