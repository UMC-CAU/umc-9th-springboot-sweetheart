package com.example.umc9th.domain.mission.service;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.domain.mission.dto.MissionRequest;
import com.example.umc9th.domain.mission.dto.MissionResponse;
import com.example.umc9th.domain.mission.entity.Mission;
import com.example.umc9th.domain.mission.entity.mapping.MemberMission;
import com.example.umc9th.domain.mission.repository.MemberMissionRepository;
import com.example.umc9th.domain.mission.repository.MissionRepository;
import com.example.umc9th.domain.store.entity.Store;
import com.example.umc9th.domain.store.repository.StoreRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미션 Command Service (CUD 작업)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionCommandService {

    private final MissionRepository missionRepository;
    private final MemberRepository memberRepository;
    private final MemberMissionRepository memberMissionRepository;
    private final StoreRepository storeRepository;

    /**
     * 가게에 미션 추가하기
     * @param request 미션 추가 요청 DTO
     * @return 생성된 미션 정보
     */
    public MissionResponse.CreateMission createMission(MissionRequest.CreateMissionDTO request) {
        log.info("[MissionCommandService.createMission] storeId: {}, name: {}, deadline: {}",
                request.getStoreId(), request.getName(), request.getDeadline());

        // 가게 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        // 미션 엔티티 생성 및 저장
        Mission mission = Mission.builder()
                .name(request.getName())
                .deadline(request.getDeadline())
                .conditional(request.getConditional())
                .point(request.getPoint())
                .store(store)
                .build();

        Mission savedMission = missionRepository.save(mission);

        log.info("[MissionCommandService.createMission] 미션 추가 완료 - missionId: {}", savedMission.getId());

        return MissionResponse.CreateMission.from(savedMission);
    }

    /**
     * 미션 도전하기 (미션을 도전 중인 미션에 추가)
     * @param request 미션 도전 요청 DTO
     * @return 생성된 도전 미션 정보
     */
    public MissionResponse.ChallengeMission challengeMission(MissionRequest.ChallengeMissionDTO request) {
        log.info("[MissionCommandService.challengeMission] missionId: {}, memberId: {}",
                request.getMissionId(), request.getMemberId());

        // 회원 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 미션 조회 (Validation에서 이미 검증했지만, 엔티티가 필요하므로 조회)
        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // MemberMission 엔티티 생성 및 저장 (기본 상태: AVAILABLE)
        MemberMission memberMission = MemberMission.builder()
                .member(member)
                .mission(mission)
                .build();

        MemberMission savedMemberMission = memberMissionRepository.save(memberMission);

        log.info("[MissionCommandService.challengeMission] 미션 도전 완료 - memberMissionId: {}, status: {}",
                savedMemberMission.getId(), savedMemberMission.getStatus());

        return MissionResponse.ChallengeMission.from(savedMemberMission);
    }
}
