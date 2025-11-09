package com.example.umc9th.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 컨트롤러
 *
 * 디스코드 웹훅 알림 등 다양한 기능을 테스트하기 위한 엔드포인트를 제공합니다.
 *
 * ⚠️ 보안상의 이유로 local, dev 환경에서만 활성화됩니다.
 * prod 환경에서는 자동으로 비활성화되어 접근할 수 없습니다.
 */
@Profile({"local", "dev"})  // local, dev 환경에서만 활성화
@Slf4j
@Tag(name = "Test", description = "테스트용 API (local, dev 환경 전용)")
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 500 에러를 강제로 발생시키는 테스트 엔드포인트
     *
     * 디스코드 웹훅 알림이 정상적으로 전송되는지 테스트합니다.
     */
    @Operation(
            summary = "500 에러 발생 테스트",
            description = "RuntimeException을 강제로 발생시켜 디스코드 웹훅 알림을 테스트합니다. " +
                    "웹훅이 활성화된 환경(dev, prod)에서만 알림이 전송됩니다."
    )
    @GetMapping("/error")
    public String throwError() {
        log.info("[TestController.throwError] 500 에러 테스트 시작");

        // 강제로 RuntimeException 발생
        throw new RuntimeException("디스코드 웹훅 알림 테스트를 위한 의도적인 에러입니다!");
    }

    /**
     * NullPointerException을 발생시키는 테스트 엔드포인트
     */
    @Operation(
            summary = "NullPointerException 발생 테스트",
            description = "NullPointerException을 강제로 발생시켜 디스코드 웹훅 알림을 테스트합니다."
    )
    @GetMapping("/error/npe")
    public String throwNullPointerException() {
        log.info("[TestController.throwNullPointerException] NPE 테스트 시작");

        String nullString = null;
        // 강제로 NullPointerException 발생
        return nullString.toUpperCase();
    }

    /**
     * ArrayIndexOutOfBoundsException을 발생시키는 테스트 엔드포인트
     */
    @Operation(
            summary = "ArrayIndexOutOfBoundsException 발생 테스트",
            description = "ArrayIndexOutOfBoundsException을 강제로 발생시켜 디스코드 웹훅 알림을 테스트합니다."
    )
    @GetMapping("/error/array")
    public String throwArrayIndexOutOfBoundsException() {
        log.info("[TestController.throwArrayIndexOutOfBoundsException] 배열 인덱스 에러 테스트 시작");

        int[] array = {1, 2, 3};
        // 강제로 ArrayIndexOutOfBoundsException 발생
        return String.valueOf(array[10]);
    }

    /**
     * 정상 응답 테스트
     */
    @Operation(
            summary = "정상 응답 테스트",
            description = "에러 없이 정상적으로 응답하는 엔드포인트입니다."
    )
    @GetMapping("/ok")
    public String ok() {
        log.info("[TestController.ok] 정상 응답 테스트");
        return "정상 응답입니다!";
    }
}
