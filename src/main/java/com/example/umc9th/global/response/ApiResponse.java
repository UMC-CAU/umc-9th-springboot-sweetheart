package com.example.umc9th.global.response;

import com.example.umc9th.global.response.code.BaseCode;
import com.example.umc9th.global.response.code.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

/**
 * 통일된 API 응답 형식을 위한 래퍼(Wrapper) 클래스
 *
 * 모든 API 응답은 이 클래스를 통해 일관된 형식으로 반환됩니다.
 *
 * 응답 형식:
 * {
 *   "isSuccess": true,
 *   "code": "MEMBER_200",
 *   "message": "회원 조회 성공",
 *   "data": { ... }
 * }
 *
 * 제네릭 타입 T:
 * - 실제 반환할 데이터의 타입을 의미합니다
 * - 예: ApiResponse<MemberResponse>, ApiResponse<List<MemberResponse>>
 * - data가 없는 경우 ApiResponse<Void> 사용 가능
 *
 * @param <T> 응답 데이터의 타입
 */
@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "data"})  // JSON 필드 순서 지정
public class ApiResponse<T> {

    /**
     * 성공 여부
     * - true: 성공 (HTTP 2xx)
     * - false: 실패 (HTTP 4xx, 5xx)
     */
    private final boolean isSuccess;

    /**
     * 응답 코드
     * - 성공: SuccessCode enum의 code 값 (예: "MEMBER_200")
     * - 실패: ErrorCode enum의 code 값 (예: "MEMBER_404")
     */
    private final String code;

    /**
     * 응답 메시지
     * - 사용자에게 보여줄 수 있는 한글 메시지
     * - 예: "회원 조회 성공", "회원을 찾을 수 없습니다"
     */
    private final String message;

    /**
     * 실제 응답 데이터
     * - 성공 시: 요청한 데이터 (DTO 객체, List 등)
     * - 실패 시: null (JsonInclude.NON_NULL로 인해 JSON에서 제외됨)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)  // data가 null이면 JSON에 포함하지 않음
    private final T data;


    // ===== 생성자 =====
    /**
     * 기본 생성자 (private)
     * 외부에서 직접 생성하지 못하도록 막고, 정적 팩토리 메서드(onSuccess, onFailure)를 통해서만 생성하도록 강제합니다.
     *
     * 왜 이렇게 할까요?
     * - 일관성: 항상 onSuccess() 또는 onFailure()를 사용하도록 강제
     * - 가독성: 코드만 봐도 성공/실패 응답인지 명확히 알 수 있음
     * - 안전성: 잘못된 조합(isSuccess=true인데 ErrorCode 사용 등)을 방지
     */
    private ApiResponse(boolean isSuccess, BaseCode code, T data) {
        this.isSuccess = isSuccess;
        this.code = code.getCode();
        this.message = code.getMessage();
        this.data = data;
    }


    // ===== 성공 응답 생성 메서드 =====
    /**
     * 데이터가 있는 성공 응답 생성
     *
     * 사용 예시:
     * <pre>
     * MemberResponse memberResponse = new MemberResponse(...);
     * return ApiResponse.onSuccess(SuccessCode.MEMBER_OK, memberResponse);
     * </pre>
     *
     * @param successCode 성공 코드 (SuccessCode enum)
     * @param data        응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode, T data) {
        return new ApiResponse<>(true, successCode, data);
    }

    /**
     * 데이터가 없는 성공 응답 생성
     *
     * 사용 예시:
     * <pre>
     * // 삭제 성공 시처럼 반환할 데이터가 없을 때
     * return ApiResponse.onSuccess(SuccessCode.MEMBER_DELETED);
     * </pre>
     *
     * @param successCode 성공 코드 (SuccessCode enum)
     * @return 성공 응답 객체 (data는 null)
     */
    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode) {
        return new ApiResponse<>(true, successCode, null);
    }


    // ===== 실패 응답 생성 메서드 =====
    /**
     * 실패 응답 생성
     *
     * 사용 예시:
     * <pre>
     * // GlobalExceptionHandler에서 사용
     * return ApiResponse.onFailure(ErrorCode.MEMBER_NOT_FOUND);
     * </pre>
     *
     * @param errorCode 에러 코드 (ErrorCode enum)
     * @return 실패 응답 객체 (data는 null)
     */
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode) {
        return new ApiResponse<>(false, errorCode, null);
    }

    /**
     * 커스텀 메시지를 포함한 실패 응답 생성
     *
     * ErrorCode의 기본 메시지 대신 상황에 맞는 구체적인 메시지를 제공할 때 사용합니다.
     *
     * 사용 예시:
     * <pre>
     * // Validation 에러에서 필드명을 포함한 메시지를 보여줄 때
     * String customMessage = "이메일 형식이 올바르지 않습니다: " + email;
     * return ApiResponse.onFailure(ErrorCode.VALIDATION_FAILED, customMessage);
     * </pre>
     *
     * @param errorCode      에러 코드 (ErrorCode enum)
     * @param customMessage  커스텀 에러 메시지
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, String customMessage) {
        return new ApiResponse<T>(false, errorCode, null) {
            @Override
            public String getMessage() {
                return customMessage;  // 기본 메시지 대신 커스텀 메시지 반환
            }
        };
    }


    // ===== 편의 메서드 =====
    /**
     * 기본 성공 응답 (OK)
     *
     * 특별한 성공 코드가 필요 없을 때 사용하는 간편 메서드
     *
     * 사용 예시:
     * <pre>
     * return ApiResponse.ok(memberResponse);
     * </pre>
     */
    public static <T> ApiResponse<T> ok(T data) {
        return onSuccess(SuccessCode.OK, data);
    }

    /**
     * 생성 성공 응답 (CREATED)
     *
     * POST 요청으로 새 리소스 생성 시 사용하는 간편 메서드
     *
     * 사용 예시:
     * <pre>
     * // 회원가입 성공
     * return ApiResponse.created(newMemberResponse);
     * </pre>
     */
    public static <T> ApiResponse<T> created(T data) {
        return onSuccess(SuccessCode.CREATED, data);
    }
}
