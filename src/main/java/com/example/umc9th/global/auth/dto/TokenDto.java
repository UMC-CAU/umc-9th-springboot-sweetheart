package com.example.umc9th.global.auth.dto;

/**
 * Token 관련 DTO 클래스 모음
 */
public class TokenDto {

    /**
     * 토큰 응답 DTO
     * - 로그인 시 Access Token + Refresh Token 반환
     * - Token Refresh 시 새 Access Token 반환
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {
        /**
         * Access Token만 있는 응답 생성 (Token Refresh 시)
         */
        public static TokenResponse ofAccessOnly(String accessToken) {
            return new TokenResponse(accessToken, null);
        }

        /**
         * 둘 다 있는 응답 생성 (로그인 시)
         */
        public static TokenResponse of(String accessToken, String refreshToken) {
            return new TokenResponse(accessToken, refreshToken);
        }
    }

    /**
     * Token Refresh 요청 DTO
     */
    public record RefreshRequest(
            String refreshToken
    ) {
    }

    /**
     * 로그아웃 요청 DTO (선택)
     */
    public record LogoutRequest(
            String refreshToken  // 또는 Authorization 헤더 사용
    ) {
    }
}
