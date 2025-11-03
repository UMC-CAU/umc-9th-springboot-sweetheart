package com.example.umc9th.domain.member.dto;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.global.auth.enums.SocialType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class MemberResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Basic {
        private Long id;
        private String name;
        private Gender gender;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birth;

        private String address;
        private String detailAddress;
        private String email;
        private String phoneNumber;
        private Integer point;
        private SocialType socialType;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        public static Basic from(Member member) {
            return Basic.builder()
                    .id(member.getId())
                    .name(member.getName())
                    .gender(member.getGender())
                    .birth(member.getBirth())
                    .address(member.getAddress())
                    .detailAddress(member.getDetailAddress())
                    .email(member.getEmail())
                    .phoneNumber(member.getPhoneNumber())
                    .point(member.getPoint())
                    .socialType(member.getSocialType())
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String name;
        private Gender gender;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birth;

        private String address;
        private String detailAddress;
        private String email;
        private String phoneNumber;
        private Integer point;
        private SocialType socialType;
        private List<FoodName> foodPreferences;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        public static Detail from(Member member) {
            return Detail.builder()
                    .id(member.getId())
                    .name(member.getName())
                    .gender(member.getGender())
                    .birth(member.getBirth())
                    .address(member.getAddress())
                    .detailAddress(member.getDetailAddress())
                    .email(member.getEmail())
                    .phoneNumber(member.getPhoneNumber())
                    .point(member.getPoint())
                    .socialType(member.getSocialType())
                    .foodPreferences(
                            member.getMemberFoodList().stream()
                                    .map(memberFood -> memberFood.getFood().getName())
                                    .collect(Collectors.toList())
                    )
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String name;
        private String email;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        public static Summary from(Member member) {
            return Summary.builder()
                    .id(member.getId())
                    .name(member.getName())
                    .email(member.getEmail())
                    .createdAt(member.getCreatedAt())
                    .build();
        }
    }
}
