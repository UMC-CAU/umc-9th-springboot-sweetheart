package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT DISTINCT m FROM Member m LEFT JOIN FETCH m.memberFoodList")
    List<Member> findAllWithMemberFoods();

    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food")
    List<Member> findAllWithFoods();

    @Query("SELECT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food " +
           "WHERE m.id = :id")
    Optional<Member> findByIdWithFoods(@Param("id") Long id);

    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.memberFoodList mf " +
           "LEFT JOIN FETCH mf.food " +
           "WHERE m.name = :name")
    List<Member> findByNameWithFoods(@Param("name") String name);
}
