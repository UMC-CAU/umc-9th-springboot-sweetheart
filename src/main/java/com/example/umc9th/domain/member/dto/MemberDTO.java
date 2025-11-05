package com.example.umc9th.domain.member.dto;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class MemberDTO {

    /**
     * 회원 목록 조회용 DTO (간단한 정보만)
     */
    @Getter
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String name;
        private Gender gender;
        private String address;

        public static Summary from(Member member) {
            return new Summary(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getAddress()
            );
        }
    }

    /**
     * 회원 상세 조회용 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String name;
        private Gender gender;
        private LocalDate birth;
        private String address;
        private String detailAddress;
        private String email;
        private Integer point;

        public static Detail from(Member member) {
            return new Detail(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getBirth(),
                member.getAddress(),
                member.getDetailAddress(),
                member.getEmail(),
                member.getPoint()
            );
        }
    }

    /**
     * 회원 + 선호 음식 목록 (DTO 안에 DTO 예시)
     */
    @Getter
    @AllArgsConstructor
    public static class WithFoods {
        private Long id;
        private String name;
        private Gender gender;

        // ⭐ DTO 안에 DTO 리스트
        private List<FoodInfo> foods;

        public static WithFoods from(Member member) {
            return new WithFoods(
                member.getId(),
                member.getName(),
                member.getGender(),
                // MemberFood Entity → FoodInfo DTO 변환
                member.getMemberFoodList().stream()
                    .map(mf -> FoodInfo.from(mf.getFood()))
                    .toList()
            );
        }

        /**
         * 중첩 DTO: 음식 정보
         */
        @Getter
        @AllArgsConstructor
        public static class FoodInfo {
            private Long id;
            private String foodName;

            public static FoodInfo from(com.example.umc9th.domain.member.entity.Food food) {
                return new FoodInfo(
                    food.getId(),
                    food.getName().name() // Food 엔티티는 getName() 사용
                );
            }
        }
    }

    /**
     * 회원 생성 요청 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private Gender gender;
        private LocalDate birth;
        private String address;
        private String detailAddress;
        private String email;
        private String phoneNumber;
        private List<Long> foodIds; // 선호 음식 ID 리스트
    }

    /**
     * 회원 수정 요청 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class UpdateRequest {
        private String address;
        private String detailAddress;
        private String phoneNumber;
    }
}
