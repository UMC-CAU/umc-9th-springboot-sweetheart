package com.example.umc9th.global.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.example.umc9th.global.notification.DiscordWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.ValidationError;
import com.example.umc9th.global.response.code.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final DiscordWebhookService discordWebhookService;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");

        log.error("[CustomException] path: {}, traceId: {}, code: {}, message: {}",
                path, traceId, e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.onFailure(e.getErrorCode(), path, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");

        List<ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.error("[Validation Failed] path: {}, traceId: {}, errors: {}", path, traceId, errors);

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.VALIDATION_FAILED, errors, path, traceId));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");
        String errorMessage = String.format("필수 파라미터 '%s'가 누락되었습니다", e.getParameterName());

        log.error("[Missing Parameter] path: {}, traceId: {}, message: {}", path, traceId, errorMessage);

        return ResponseEntity
                .status(ErrorCode.MISSING_PARAMETER.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.MISSING_PARAMETER, errorMessage));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");
        String errorMessage = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다",
                e.getName(),
                e.getValue());

        log.error("[Type Mismatch] path: {}, traceId: {}, message: {}", path, traceId, errorMessage);

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.BAD_REQUEST, errorMessage));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");
        String errorMessage = String.format("'%s %s' 엔드포인트를 찾을 수 없습니다",
                e.getHttpMethod(),
                e.getRequestURL());

        log.error("[Not Found] path: {}, traceId: {}, message: {}", path, traceId, errorMessage);

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.NOT_FOUND, errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(
            Exception e,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-ID");

        log.error("[Unexpected Exception] path: {}, traceId: {}, type: {}, message: {}",
                path, traceId, e.getClass().getSimpleName(), e.getMessage(), e);

        // 디스코드 웹훅으로 500 에러 알림 전송 (비동기)
        sendDiscordNotification(path, e, traceId);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR, path, traceId));
    }

    /**
     * 디스코드로 에러 알림 전송
     */
    private void sendDiscordNotification(String path, Exception e, String traceId) {
        try {
            String errorMessage = e.getMessage();
            String exceptionType = e.getClass().getSimpleName();
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            discordWebhookService.sendErrorNotification(
                    path, errorMessage, exceptionType, traceId, timestamp
            );
        } catch (Exception notificationException) {
            // 알림 전송 실패 시 로그만 남기고 원래 에러 처리는 계속 진행
            log.error("[Discord Notification Failed] 디스코드 알림 전송 중 오류 발생: {}",
                    notificationException.getMessage());
        }
    }
}
