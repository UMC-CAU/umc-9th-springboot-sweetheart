package com.example.umc9th.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 성공 응답 코드
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {

    // 공통
    OK(200, "COMMON_200", "성공"),
    CREATED(201, "COMMON_201", "생성 성공"),

    // Member
    MEMBER_OK(200, "MEMBER_200", "회원 조회 성공"),
    MEMBER_LIST_OK(200, "MEMBER_LIST_200", "회원 목록 조회 성공"),
    MEMBER_CREATED(201, "MEMBER_201", "회원 생성 성공"),
    MEMBER_UPDATED(200, "MEMBER_200", "회원 수정 성공"),
    MEMBER_DELETED(200, "MEMBER_200", "회원 삭제 성공"),

    // Review
    REVIEW_CREATED(201, "REVIEW_201", "리뷰 작성 성공"),
    REVIEW_LIST_OK(200, "REVIEW_LIST_200", "리뷰 목록 조회 성공"),

    // Mission
    MISSION_CREATED(201, "MISSION_201", "미션 생성 성공"),
    MISSION_COMPLETED(200, "MISSION_200", "미션 완료 성공");

    private final int status;
    private final String code;
    private final String message;
}
