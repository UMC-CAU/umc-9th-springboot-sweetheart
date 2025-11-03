package com.example.umc9th.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 *
 * @RestControllerAdvice 어노테이션의 역할:
 * - 모든 @RestController에서 발생하는 예외를 한 곳에서 처리
 * - @ControllerAdvice + @ResponseBody의 조합
 * - JSON 형식으로 에러 응답을 자동 변환
 *
 * 왜 이렇게 할까요?
 * - Controller마다 try-catch를 작성할 필요 없음 (코드 중복 제거)
 * - 모든 에러 응답이 ApiResponse 형식으로 통일됨
 * - 에러 처리 로직을 한 곳에서 관리 (유지보수성 향상)
 *
 * @Slf4j: Lombok이 제공하는 로깅 어노테이션
 * - log.error(), log.info() 등을 사용할 수 있게 해줍니다
 * - 프로덕션 환경에서 에러 추적에 필수적입니다
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     *
     * 우리가 직접 발생시킨 비즈니스 로직 예외를 처리합니다.
     *
     * 처리 흐름:
     * 1. Service에서 throw new CustomException(ErrorCode.MEMBER_NOT_FOUND)
     * 2. 이 메서드가 자동으로 catch
     * 3. ApiResponse 형식으로 변환하여 반환
     *
     * @param e CustomException 인스턴스
     * @return 에러 응답 (ApiResponse 형식)
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        // 에러 로그 출력 (운영 환경에서 문제 파악에 중요)
        log.error("[CustomException] code: {}, message: {}",
                e.getErrorCode().getCode(),
                e.getMessage());

        // ErrorCode에서 HTTP 상태 코드를 가져와서 ResponseEntity 생성
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.onFailure(e.getErrorCode()));
    }


    /**
     * @Valid 검증 실패 시 발생하는 예외 처리
     *
     * @Valid는 DTO의 필드 검증 어노테이션입니다:
     * - @NotNull: null 불가
     * - @NotBlank: 빈 문자열 불가
     * - @Email: 이메일 형식 검증
     * - @Size: 길이 제한
     * - @Min, @Max: 숫자 범위 제한 등
     *
     * 사용 예시:
     * <pre>
     * public class MemberRequest {
     *     @NotBlank(message = "이름은 필수입니다")
     *     private String name;
     *
     *     @Email(message = "올바른 이메일 형식이 아닙니다")
     *     private String email;
     * }
     *
     * // Controller
     * public ApiResponse<Member> createMember(@Valid @RequestBody MemberRequest request) {
     *     // 검증 실패 시 이 메서드가 자동으로 처리
     * }
     * </pre>
     *
     * @param e MethodArgumentNotValidException 인스턴스
     * @return 에러 응답 (검증 실패한 필드 정보 포함)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 검증 실패한 필드들을 추출
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> String.format("%s: %s",
                        fieldError.getField(),           // 필드명 (예: "email")
                        fieldError.getDefaultMessage())) // 에러 메시지 (예: "올바른 이메일 형식이 아닙니다")
                .collect(Collectors.joining(", "));      // 쉼표로 연결

        log.error("[Validation Failed] {}", errorMessage);

        // 커스텀 메시지와 함께 응답
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.VALIDATION_FAILED, errorMessage));
    }


    /**
     * 필수 파라미터 누락 시 발생하는 예외 처리
     *
     * 발생 케이스:
     * - @RequestParam(required = true)인 파라미터가 요청에 없을 때
     *
     * 예시:
     * <pre>
     * // GET /api/members/search?name=홍길동  ← OK
     * // GET /api/members/search              ← 이 예외 발생
     *
     * @GetMapping("/search")
     * public ApiResponse<List<Member>> search(@RequestParam String name) { ... }
     * </pre>
     *
     * @param e MissingServletRequestParameterException 인스턴스
     * @return 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다", e.getParameterName());

        log.error("[Missing Parameter] {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.MISSING_PARAMETER.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.MISSING_PARAMETER, errorMessage));
    }


    /**
     * 파라미터 타입 불일치 시 발생하는 예외 처리
     *
     * 발생 케이스:
     * - @PathVariable이나 @RequestParam의 타입이 맞지 않을 때
     *
     * 예시:
     * <pre>
     * // GET /api/members/123   ← OK (Long 타입)
     * // GET /api/members/abc   ← 이 예외 발생 (문자열을 Long으로 변환 불가)
     *
     * @GetMapping("/{id}")
     * public ApiResponse<Member> getMember(@PathVariable Long id) { ... }
     * </pre>
     *
     * @param e MethodArgumentTypeMismatchException 인스턴스
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String errorMessage = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다",
                e.getName(),
                e.getValue());

        log.error("[Type Mismatch] {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.BAD_REQUEST, errorMessage));
    }


    /**
     * 존재하지 않는 엔드포인트 호출 시 발생하는 예외 처리
     *
     * 발생 케이스:
     * - 정의되지 않은 URL로 요청 시
     *
     * 예시:
     * - GET /api/members     ← OK (정의된 엔드포인트)
     * - GET /api/membersss   ← 이 예외 발생 (오타로 잘못된 URL)
     *
     * 주의: application.yml에 다음 설정 필요
     * <pre>
     * spring:
     *   mvc:
     *     throw-exception-if-no-handler-found: true
     *   web:
     *     resources:
     *       add-mappings: false
     * </pre>
     *
     * @param e NoHandlerFoundException 인스턴스
     * @return 에러 응답
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException e) {
        String errorMessage = String.format("'%s %s' 엔드포인트를 찾을 수 없습니다",
                e.getHttpMethod(),
                e.getRequestURL());

        log.error("[Not Found] {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.NOT_FOUND, errorMessage));
    }


    /**
     * 그 외 모든 예상하지 못한 예외 처리
     *
     * 위에서 처리하지 못한 모든 예외를 catch합니다.
     * - NullPointerException
     * - IllegalArgumentException
     * - 데이터베이스 관련 예외
     * - 기타 RuntimeException 등
     *
     * 왜 이게 필요할까요?
     * - 예상치 못한 에러가 발생해도 일관된 형식으로 응답
     * - 서버 에러의 상세 정보가 클라이언트에 노출되는 것을 방지 (보안)
     * - 로그를 통해 문제를 추적할 수 있음
     *
     * @param e 발생한 예외
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception e) {
        // 스택 트레이스까지 로그에 출력 (문제 원인 파악을 위해)
        log.error("[Unexpected Exception] type: {}, message: {}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e);  // 세 번째 파라미터로 예외를 전달하면 스택 트레이스가 로그에 포함됨

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR));
    }


    // ===== 추가로 처리할 수 있는 예외들 (필요 시 주석 해제) =====

    /**
     * HTTP Method 불일치 시 처리
     *
     * 예: POST만 지원하는 엔드포인트에 GET 요청을 보낼 때
     */
    /*
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        String errorMessage = String.format("지원하지 않는 HTTP 메서드입니다: %s", e.getMethod());
        log.error("[Method Not Allowed] {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.METHOD_NOT_ALLOWED, errorMessage));
    }
    */

    /**
     * JSON 파싱 실패 시 처리
     *
     * 예: 잘못된 JSON 형식을 요청 body로 보냈을 때
     */
    /*
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("[Invalid JSON] {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.BAD_REQUEST, "올바르지 않은 JSON 형식입니다"));
    }
    */
}
