package com.example.umc9th.global.auth.oauth2;

import com.example.umc9th.global.auth.CustomUserDetails;
import com.example.umc9th.global.auth.jwt.JwtUtil;
import com.example.umc9th.global.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 처리하는 핸들러
 * - JWT Access Token + Refresh Token 생성
 * - 프론트엔드로 리다이렉트 (토큰 전달)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Value("${oauth2.redirect.success-url:http://localhost:3000/oauth/callback}")
    private String frontendRedirectUrl;

    /**
     * OAuth2 로그인 성공 시 호출
     * - JWT 토큰 생성
     * - 프론트엔드로 리다이렉트 (쿼리 파라미터로 토큰 전달)
     *
     * @param request        요청 객체
     * @param response       응답 객체
     * @param authentication 인증 정보 (사용자 정보 포함)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("[OAuth2SuccessHandler.onAuthenticationSuccess] OAuth2 로그인 성공");

        // 인증된 사용자 정보 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();  // 이메일
        Long memberId = userDetails.getMemberId();

        log.info("[OAuth2SuccessHandler.onAuthenticationSuccess] 사용자 - ID: {}, email: {}", memberId, email);

        // JWT Access Token + Refresh Token 생성
        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // Refresh Token DB에 저장
        authService.saveRefreshToken(userDetails.getMember(), refreshToken);

        log.info("[OAuth2SuccessHandler.onAuthenticationSuccess] JWT 토큰 발급 완료 - email: {}", email);

        // 프론트엔드로 리다이렉트 (쿼리 파라미터로 토큰 전달)
        // 예: http://localhost:3000/oauth/callback?accessToken=...&refreshToken=...
        String redirectUrl = String.format("%s?accessToken=%s&refreshToken=%s&memberId=%d",
                frontendRedirectUrl, accessToken, refreshToken, memberId);

        log.info("[OAuth2SuccessHandler.onAuthenticationSuccess] 리다이렉트 URL: {}", redirectUrl);

        // 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
