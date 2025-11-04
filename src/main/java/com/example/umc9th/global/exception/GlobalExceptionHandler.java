package com.example.umc9th.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
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
@RestControllerAdvice
public class GlobalExceptionHandler {

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

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR, path, traceId));
    }
}
