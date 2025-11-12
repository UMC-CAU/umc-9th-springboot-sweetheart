package com.example.umc9th.global.response.code;

/**
 * 모든 응답 코드가 구현해야 하는 기본 인터페이스
 */
public interface BaseCode {

    int getStatus();
    String getCode();
    String getMessage();
}
