package com.example.umc9th.global.exception;

import com.example.umc9th.global.response.code.ErrorCode;
import lombok.Getter;

/**
 * 애플리케이션 전역에서 사용할 커스텀 예외 클래스
 *
 * RuntimeException을 상속받아 Unchecked Exception으로 만듭니다.
 * 왜 Unchecked Exception일까요?
 * - Checked Exception(IOException 등)은 명시적으로 try-catch 또는 throws를 강제합니다
 * - 하지만 비즈니스 로직에서 발생하는 대부분의 예외는 복구가 불가능하므로
 *   RuntimeException을 사용하여 코드를 간결하게 유지합니다
 * - GlobalExceptionHandler에서 한 곳에서 모든 예외를 처리합니다
 *
 * 사용 예시:
 * <pre>
 * // Service에서 회원을 찾지 못했을 때
 * Member member = memberRepository.findById(id)
 *     .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
 * </pre>
 */
@Getter
public class CustomException extends RuntimeException {

    /**
     * 에러 코드
     * ErrorCode enum을 통해 어떤 종류의 에러인지 식별합니다
     */
    private final ErrorCode errorCode;

    /**
     * 기본 생성자
     *
     * @param errorCode 발생한 에러의 종류
     */
    public CustomException(ErrorCode errorCode) {
        // RuntimeException의 message로 errorCode의 메시지를 전달
        // 이렇게 하면 로그에서 예외 메시지를 바로 확인할 수 있습니다
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * ErrorCode의 기본 메시지 대신 더 구체적인 메시지를 제공할 때 사용합니다.
     *
     * 사용 예시:
     * <pre>
     * // 유효성 검증 실패 시 구체적인 필드명을 포함
     * throw new CustomException(
     *     ErrorCode.VALIDATION_FAILED,
     *     "이메일 형식이 올바르지 않습니다: " + email
     * );
     * </pre>
     *
     * @param errorCode     에러 코드
     * @param customMessage 커스텀 에러 메시지
     */
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * 원인(cause)을 포함한 생성자
     *
     * 다른 예외를 래핑할 때 사용합니다.
     * 예: DB 예외를 CustomException으로 변환하면서 원래 예외 정보는 유지
     *
     * 사용 예시:
     * <pre>
     * try {
     *     // DB 작업
     * } catch (SQLException e) {
     *     throw new CustomException(ErrorCode.DATABASE_ERROR, e);
     * }
     * </pre>
     *
     * @param errorCode 에러 코드
     * @param cause     원인이 된 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * HTTP 상태 코드를 반환하는 편의 메서드
     *
     * @return HTTP 상태 코드 (400, 404, 500 등)
     */
    public int getStatus() {
        return errorCode.getStatus();
    }

    /**
     * 에러 코드 문자열을 반환하는 편의 메서드
     *
     * @return 에러 코드 (MEMBER_404, COMMON_500 등)
     */
    public String getCode() {
        return errorCode.getCode();
    }
}
