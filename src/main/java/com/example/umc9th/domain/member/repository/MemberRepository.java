package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @EntityGraph(attributePaths = {"memberFoodList"})
    @Query("SELECT DISTINCT m FROM Member m")
    List<Member> findAllWithMemberFoods();

    @EntityGraph(attributePaths = {"memberFoodList", "memberFoodList.food"})
    @Query("SELECT DISTINCT m FROM Member m")
    List<Member> findAllWithFoods();

    @EntityGraph(attributePaths = {"memberFoodList", "memberFoodList.food"})
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdWithFoods(@Param("id") Long id);

    @EntityGraph(attributePaths = {"memberFoodList", "memberFoodList.food"})
    @Query("SELECT DISTINCT m FROM Member m WHERE m.name = :name")
    List<Member> findByNameWithFoods(@Param("name") String name);

    /**
     * 이메일 중복 확인
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 소셜 UID 중복 확인
     *
     * @param socialUid 소셜 UID
     * @return 존재 여부
     */
    boolean existsBySocialUid(String socialUid);

    /**
     * 이메일로 회원 조회
     *
     * @param email 이메일
     * @return 회원 Optional
     */
    Optional<Member> findByEmail(String email);

    /**
     * 소셜 UID로 회원 조회
     *
     * @param socialUid 소셜 UID
     * @return 회원 Optional
     */
    Optional<Member> findBySocialUid(String socialUid);
}
