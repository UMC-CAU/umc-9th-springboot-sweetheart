package com.example.umc9th.domain.member.dto;

import com.example.umc9th.domain.member.enums.FoodName;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.global.auth.enums.SocialType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class MemberRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
        private String name;

        @NotNull(message = "성별은 필수입니다")
        private Gender gender;

        @NotNull(message = "생년월일은 필수입니다")
        @Past(message = "생년월일은 과거 날짜여야 합니다")
        private LocalDate birth;

        @NotBlank(message = "주소는 필수입니다")
        private String address;

        @NotBlank(message = "상세 주소는 필수입니다")
        private String detailAddress;

        @NotBlank(message = "소셜 UID는 필수입니다")
        private String socialUid;

        @NotNull(message = "소셜 로그인 타입은 필수입니다")
        private SocialType socialType;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 10~11자리 숫자여야 합니다")
        private String phoneNumber;

        private List<FoodName> foodPreferences;
    }

    /**
     * 일반 로그인을 위한 회원가입 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Join {

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
        private String name;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
        private String password;

        @NotNull(message = "성별은 필수입니다")
        private Gender gender;

        @NotNull(message = "생년월일은 필수입니다")
        @Past(message = "생년월일은 과거 날짜여야 합니다")
        private LocalDate birth;

        @NotBlank(message = "주소는 필수입니다")
        private String address;

        @NotBlank(message = "상세 주소는 필수입니다")
        private String detailAddress;

        private List<FoodName> foodPreferences;
    }

    /**
     * 로그인 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
        private String name;

        private Gender gender;

        @Past(message = "생년월일은 과거 날짜여야 합니다")
        private LocalDate birth;

        private String address;

        private String detailAddress;

        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호는 10~11자리 숫자여야 합니다")
        private String phoneNumber;

        private List<FoodName> foodPreferences;
    }
}
