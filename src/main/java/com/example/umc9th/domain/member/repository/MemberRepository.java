package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberRepository - Fetch Join을 활용한 N+1 문제 해결
 *
 * N+1 문제란?
 * - Member 10명 조회 시 (1번 쿼리)
 * - 각 Member의 음식 정보 조회 시 10번 추가 쿼리 발생
 * - 총 11번(1+N)의 쿼리 발생 → 성능 저하
 *
 * Fetch Join 해결법:
 * - JOIN FETCH 사용으로 한 번의 쿼리로 모든 데이터 조회
 * - Member와 연관된 MemberFood, Food를 한 번에 가져옴
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // ========== Fetch Join으로 N+1 문제 해결 ==========

    /**
     * Member 전체 조회 + 선호 음식 정보 (1단계 Fetch Join)
     *
     * JPQL: Member와 MemberFood를 LEFT JOIN FETCH로 한 번에 조회
     * DISTINCT: OneToMany 관계에서 Member 중복 제거
     *
     * 실행 SQL:
     * SELECT DISTINCT m.*, mf.*
     * FROM member m
     * LEFT JOIN member_food mf ON m.id = mf.member_id
     *
     * 효과: N+1 문제 해결 (쿼리 1번으로 완료)
     */
    @Query("SELECT DISTINCT m FROM Member m LEFT JOIN FETCH m.memberFoodList")
    List<Member> findAllWithMemberFoods();

    /**
     * Member 전체 조회 + 선호 음식 상세 정보 (2단계 Fetch Join) ⭐ 가장 많이 사용
     *
     * JPQL: Member -> MemberFood -> Food까지 모두 JOIN FETCH
     *
     * 실행 SQL:
     * SELECT DISTINCT m.*, mf.*, f.*
     * FROM member m
     * LEFT JOIN member_food mf ON m.id = mf.member_id
     * LEFT JOIN food f ON mf.food_id = f.id
     *
     * 효과: Member가 Food 정보까지 접근해도 추가 쿼리 없음!
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food")
    List<Member> findAllWithFoods();

    /**
     * 특정 Member 조회 + 선호 음식 상세 정보 (ID 기반)
     *
     * 효과: 단일 Member 조회 시에도 N+1 문제 없음
     */
    @Query("SELECT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food " +
           "WHERE m.id = :id")
    Optional<Member> findByIdWithFoods(@Param("id") Long id);

    /**
     * 이름으로 Member 조회 + 선호 음식 정보
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food " +
           "WHERE m.name = :name")
    List<Member> findByNameWithFoods(@Param("name") String name);
}
