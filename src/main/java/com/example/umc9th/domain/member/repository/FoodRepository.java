package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Food;
import com.example.umc9th.domain.member.enums.FoodName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * FoodName으로 Food 엔티티 조회
     * @param name 음식 이름 (KOREAN, CHINESE, JAPANESE 등)
     * @return Food 엔티티
     */
    Optional<Food> findByName(FoodName name);

    /**
     * 여러 FoodName으로 Food 엔티티 리스트 조회
     * @param names 음식 이름 리스트
     * @return Food 엔티티 리스트
     */
    List<Food> findByNameIn(List<FoodName> names);
}
