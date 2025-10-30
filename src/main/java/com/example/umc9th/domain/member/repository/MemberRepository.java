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
}
