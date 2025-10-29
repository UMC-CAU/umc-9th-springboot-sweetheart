package com.example.umc9th.domain.mission.repository;

import com.example.umc9th.domain.mission.entity.mapping.MemberMission;
import com.example.umc9th.domain.mission.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberMissionRepository extends JpaRepository<MemberMission, Long> {

    @Query("SELECT mm FROM MemberMission mm " +
           "JOIN FETCH mm.mission m " +
           "JOIN FETCH m.store s " +
           "JOIN FETCH s.food f " +
           "WHERE mm.member.id = :memberId " +
           "AND mm.status IN ('AVAILABLE', 'IN_PROGRESS') " +
           "ORDER BY m.deadline ASC")
    List<MemberMission> findAvailableAndInProgressMissions(@Param("memberId") Long memberId);

    @Query("SELECT mm FROM MemberMission mm " +
           "JOIN FETCH mm.mission m " +
           "JOIN FETCH m.store s " +
           "JOIN FETCH s.food f " +
           "WHERE mm.member.id = :memberId " +
           "AND mm.status = :status " +
           "AND mm.id > :lastId " +
           "ORDER BY mm.id ASC")
    List<MemberMission> findByMemberAndStatusWithPagination(
            @Param("memberId") Long memberId,
            @Param("status") MissionStatus status,
            @Param("lastId") Long lastId,
            @Param("pageSize") int pageSize
    );

    @Query("SELECT COUNT(mm) FROM MemberMission mm " +
           "WHERE mm.member.id = :memberId " +
           "AND mm.status = :status")
    Long countByMemberAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") MissionStatus status
    );
}
