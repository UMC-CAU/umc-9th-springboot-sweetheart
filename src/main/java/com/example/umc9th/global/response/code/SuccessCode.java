package com.example.umc9th.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 성공 응답 코드를 정의하는 Enum
 *
 * 모든 성공 응답은 이 enum을 사용하여 일관된 형식을 유지합니다.
 * 새로운 API를 추가할 때마다 여기에 성공 코드를 추가하면 됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {

    // ===== 공통 성공 코드 =====
    /**
     * 일반적인 성공 응답
     * 특별히 구체적인 성공 코드가 필요 없는 경우 사용
     */
    OK(200, "COMMON_200", "성공"),

    /**
     * 리소스 생성 성공
     * POST 요청으로 새로운 데이터를 생성했을 때 사용 (예: 회원가입, 리뷰 작성 등)
     */
    CREATED(201, "COMMON_201", "생성 성공"),


    // ===== Member 도메인 성공 코드 =====
    /**
     * 회원 단일 조회 성공
     * GET /api/members/{id} 성공 시 사용
     */
    MEMBER_OK(200, "MEMBER_200", "회원 조회 성공"),

    /**
     * 회원 목록 조회 성공
     * GET /api/members 성공 시 사용
     */
    MEMBER_LIST_OK(200, "MEMBER_LIST_200", "회원 목록 조회 성공"),

    /**
     * 회원 생성 성공
     * POST /api/members 성공 시 사용
     */
    MEMBER_CREATED(201, "MEMBER_201", "회원 생성 성공"),

    /**
     * 회원 수정 성공
     * PUT/PATCH /api/members/{id} 성공 시 사용
     */
    MEMBER_UPDATED(200, "MEMBER_200", "회원 수정 성공"),

    /**
     * 회원 삭제 성공
     * DELETE /api/members/{id} 성공 시 사용
     */
    MEMBER_DELETED(200, "MEMBER_200", "회원 삭제 성공"),


    // ===== Review 도메인 성공 코드 (예시 - 추후 구현 시 사용) =====
    REVIEW_CREATED(201, "REVIEW_201", "리뷰 작성 성공"),
    REVIEW_LIST_OK(200, "REVIEW_LIST_200", "리뷰 목록 조회 성공"),


    // ===== Mission 도메인 성공 코드 (예시 - 추후 구현 시 사용) =====
    MISSION_CREATED(201, "MISSION_201", "미션 생성 성공"),
    MISSION_COMPLETED(200, "MISSION_200", "미션 완료 성공");


    // ===== Enum 필드 =====
    /**
     * HTTP 상태 코드 (200, 201 등)
     */
    private final int status;

    /**
     * 커스텀 응답 코드 (MEMBER_200, REVIEW_201 등)
     * 프론트엔드에서 세부적인 성공 케이스를 구분할 수 있게 해줍니다
     */
    private final String code;

    /**
     * 사용자에게 보여줄 성공 메시지
     */
    private final String message;


    // BaseCode 인터페이스의 메서드들은 Lombok @Getter가 자동 생성해줍니다.
    // getStatus(), getCode(), getMessage() 메서드가 자동으로 만들어집니다.
}
