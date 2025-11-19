package com.example.umc9th.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 응답 코드
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {

    // 공통 에러 (4xx)
    BAD_REQUEST(400, "COMMON_400", "잘못된 요청입니다"),
    MISSING_PARAMETER(400, "COMMON_400", "필수 파라미터가 누락되었습니다"),
    VALIDATION_FAILED(400, "COMMON_400", "입력값 검증에 실패했습니다"),
    UNAUTHORIZED(401, "COMMON_401", "인증이 필요합니다"),
    FORBIDDEN(403, "COMMON_403", "접근 권한이 없습니다"),
    NOT_FOUND(404, "COMMON_404", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(405, "COMMON_405", "지원하지 않는 HTTP 메서드입니다"),
    CONFLICT(409, "COMMON_409", "이미 존재하는 리소스입니다"),

    // 공통 에러 (5xx)
    INTERNAL_SERVER_ERROR(500, "COMMON_500", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR(500, "COMMON_500", "데이터베이스 오류가 발생했습니다"),

    // Member
    MEMBER_NOT_FOUND(404, "MEMBER_404", "회원을 찾을 수 없습니다"),
    MEMBER_ALREADY_EXISTS(409, "MEMBER_409", "이미 존재하는 회원입니다"),
    MEMBER_DUPLICATE_EMAIL(409, "MEMBER_409", "이미 사용 중인 이메일입니다"),
    MEMBER_DUPLICATE_SOCIAL_UID(409, "MEMBER_409", "이미 가입된 소셜 계정입니다"),
    MEMBER_NAME_EMPTY(400, "MEMBER_400", "회원 이름은 필수입니다"),
    MEMBER_INVALID_EMAIL(400, "MEMBER_400", "올바른 이메일 형식이 아닙니다"),

    // Review
    REVIEW_NOT_FOUND(404, "REVIEW_404", "리뷰를 찾을 수 없습니다"),
    REVIEW_ALREADY_EXISTS(409, "REVIEW_409", "이미 리뷰를 작성했습니다"),

    // Mission
    MISSION_NOT_FOUND(404, "MISSION_404", "미션을 찾을 수 없습니다"),
    MISSION_ALREADY_COMPLETED(400, "MISSION_400", "이미 완료된 미션입니다"),

    // Store
    STORE_NOT_FOUND(404, "STORE_404", "가게를 찾을 수 없습니다"),

    // Location
    LOCATION_NOT_FOUND(404, "LOCATION_404", "지역을 찾을 수 없습니다"),

    // Food
    FOOD_NOT_FOUND(404, "FOOD_404", "음식 카테고리를 찾을 수 없습니다");

    private final int status;
    private final String code;
    private final String message;
}
