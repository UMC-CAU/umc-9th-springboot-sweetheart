package com.example.umc9th.global.response;

import com.example.umc9th.global.response.code.BaseCode;
import com.example.umc9th.global.response.code.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "timestamp", "path", "traceId", "data", "errors"})
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String path;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String traceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ValidationError> errors;

    private ApiResponse(boolean isSuccess, BaseCode code, T data) {
        this(isSuccess, code, data, null, null, null);
    }

    private ApiResponse(boolean isSuccess, BaseCode code, T data, String path, String traceId) {
        this(isSuccess, code, data, path, traceId, null);
    }

    private ApiResponse(boolean isSuccess, BaseCode code, T data, String path, String traceId, List<ValidationError> errors) {
        this.isSuccess = isSuccess;
        this.code = code.getCode();
        this.message = code.getMessage();
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.traceId = traceId;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode, T data) {
        return new ApiResponse<>(true, successCode, data);
    }

    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode) {
        return new ApiResponse<>(true, successCode, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseCode errorCode) {
        return new ApiResponse<>(false, errorCode, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, String path, String traceId) {
        return new ApiResponse<>(false, errorCode, null, path, traceId);
    }

    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, String customMessage) {
        return new ApiResponse<T>(false, errorCode, null) {
            @Override
            public String getMessage() {
                return customMessage;
            }
        };
    }

    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, List<ValidationError> errors) {
        return new ApiResponse<>(false, errorCode, null, null, null, errors);
    }

    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, List<ValidationError> errors, String path, String traceId) {
        return new ApiResponse<>(false, errorCode, null, path, traceId, errors);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return onSuccess(SuccessCode.OK, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return onSuccess(SuccessCode.CREATED, data);
    }
}
