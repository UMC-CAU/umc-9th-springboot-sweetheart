package com.example.umc9th.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 응답 코드를 정의하는 Enum
 *
 * 모든 에러 응답은 이 enum을 사용하여 일관된 형식을 유지합니다.
 * GlobalExceptionHandler에서 이 코드들을 사용하여 에러를 처리합니다.
 *
 * HTTP 상태 코드 가이드:
 * - 400: 클라이언트의 잘못된 요청 (Bad Request)
 * - 404: 리소스를 찾을 수 없음 (Not Found)
 * - 409: 리소스 충돌 (Conflict) - 예: 중복된 이메일
 * - 500: 서버 내부 오류 (Internal Server Error)
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {

    // ===== 공통 에러 코드 (4xx) =====
    /**
     * 잘못된 요청
     * 요청 파라미터 검증 실패, 잘못된 형식 등에 사용
     */
    BAD_REQUEST(400, "COMMON_400", "잘못된 요청입니다"),

    /**
     * 필수 파라미터 누락
     * @RequestParam, @PathVariable 등이 누락되었을 때 사용
     */
    MISSING_PARAMETER(400, "COMMON_400", "필수 파라미터가 누락되었습니다"),

    /**
     * 유효성 검증 실패
     * @Valid 검증 실패 시 사용 (예: 이메일 형식 오류, 길이 제한 초과 등)
     */
    VALIDATION_FAILED(400, "COMMON_400", "입력값 검증에 실패했습니다"),

    /**
     * 인증 실패
     * 로그인 실패, 토큰 없음 등에 사용
     */
    UNAUTHORIZED(401, "COMMON_401", "인증이 필요합니다"),

    /**
     * 권한 없음
     * 인증은 되었으나 해당 리소스에 접근 권한이 없을 때 사용
     */
    FORBIDDEN(403, "COMMON_403", "접근 권한이 없습니다"),

    /**
     * 리소스를 찾을 수 없음
     * 존재하지 않는 ID로 조회 시도 시 사용
     */
    NOT_FOUND(404, "COMMON_404", "요청한 리소스를 찾을 수 없습니다"),

    /**
     * HTTP 메서드 지원 안 함
     * GET만 지원하는 엔드포인트에 POST를 보낸 경우 등에 사용
     */
    METHOD_NOT_ALLOWED(405, "COMMON_405", "지원하지 않는 HTTP 메서드입니다"),

    /**
     * 리소스 충돌
     * 중복된 데이터 생성 시도 시 사용 (예: 이미 존재하는 이메일로 회원가입)
     */
    CONFLICT(409, "COMMON_409", "이미 존재하는 리소스입니다"),


    // ===== 공통 에러 코드 (5xx) =====
    /**
     * 서버 내부 오류
     * 예상하지 못한 서버 에러 발생 시 사용
     */
    INTERNAL_SERVER_ERROR(500, "COMMON_500", "서버 내부 오류가 발생했습니다"),

    /**
     * 데이터베이스 오류
     * DB 연결 실패, 쿼리 오류 등에 사용
     */
    DATABASE_ERROR(500, "COMMON_500", "데이터베이스 오류가 발생했습니다"),


    // ===== Member 도메인 에러 코드 =====
    /**
     * 회원을 찾을 수 없음
     * 존재하지 않는 회원 ID로 조회 시 사용
     */
    MEMBER_NOT_FOUND(404, "MEMBER_404", "회원을 찾을 수 없습니다"),

    /**
     * 이미 존재하는 회원
     * 중복된 이메일이나 소셜 UID로 회원가입 시도 시 사용
     */
    MEMBER_ALREADY_EXISTS(409, "MEMBER_409", "이미 존재하는 회원입니다"),

    /**
     * 회원 이름이 비어있음
     * 필수 필드인 name이 없을 때 사용
     */
    MEMBER_NAME_EMPTY(400, "MEMBER_400", "회원 이름은 필수입니다"),

    /**
     * 잘못된 이메일 형식
     * 이메일 형식이 올바르지 않을 때 사용
     */
    MEMBER_INVALID_EMAIL(400, "MEMBER_400", "올바른 이메일 형식이 아닙니다"),


    // ===== Review 도메인 에러 코드 (예시 - 추후 구현 시 사용) =====
    REVIEW_NOT_FOUND(404, "REVIEW_404", "리뷰를 찾을 수 없습니다"),
    REVIEW_ALREADY_EXISTS(409, "REVIEW_409", "이미 리뷰를 작성했습니다"),


    // ===== Mission 도메인 에러 코드 (예시 - 추후 구현 시 사용) =====
    MISSION_NOT_FOUND(404, "MISSION_404", "미션을 찾을 수 없습니다"),
    MISSION_ALREADY_COMPLETED(400, "MISSION_400", "이미 완료된 미션입니다"),


    // ===== Store 도메인 에러 코드 (예시 - 추후 구현 시 사용) =====
    STORE_NOT_FOUND(404, "STORE_404", "가게를 찾을 수 없습니다");


    // ===== Enum 필드 =====
    /**
     * HTTP 상태 코드 (400, 404, 500 등)
     */
    private final int status;

    /**
     * 커스텀 에러 코드 (MEMBER_404, REVIEW_409 등)
     * 프론트엔드에서 구체적인 에러를 식별하고 적절한 처리를 할 수 있게 해줍니다
     */
    private final String code;

    /**
     * 사용자에게 보여줄 에러 메시지
     * 프론트엔드에서 그대로 alert나 toast로 보여줄 수 있는 수준으로 작성
     */
    private final String message;


    // BaseCode 인터페이스의 메서드들은 Lombok @Getter가 자동 생성해줍니다.
    // getStatus(), getCode(), getMessage() 메서드가 자동으로 만들어집니다.
}
