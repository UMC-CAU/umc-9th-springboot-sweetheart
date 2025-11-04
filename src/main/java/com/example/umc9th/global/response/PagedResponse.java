package com.example.umc9th.global.response;

import com.example.umc9th.global.response.code.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "timestamp", "data", "pageInfo"})
public class PagedResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<T> data;

    private final PageInfo pageInfo;

    private PagedResponse(SuccessCode successCode, List<T> content, PageInfo pageInfo) {
        this.isSuccess = true;
        this.code = successCode.getCode();
        this.message = successCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.data = content;
        this.pageInfo = pageInfo;
    }

    public static <T> PagedResponse<T> of(SuccessCode successCode, List<T> content, Page<?> page) {
        PageInfo pageInfo = PageInfo.from(page);
        return new PagedResponse<>(successCode, content, pageInfo);
    }

    public static <T> PagedResponse<T> of(SuccessCode successCode, List<T> content, PageInfo pageInfo) {
        return new PagedResponse<>(successCode, content, pageInfo);
    }
}
