package com.example.umc9th.global.auth.jwt;

import com.example.umc9th.global.auth.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - 모든 HTTP 요청을 가로채서 JWT 토큰 검증
 * - 유효한 토큰이면 SecurityContext에 인증 정보 저장
 * - OncePerRequestFilter: 요청당 한 번만 실행되도록 보장
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * JWT 토큰 검증 및 인증 처리
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 다음 필터 체인
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰이 없거나 Bearer 형식이 아니면 다음 필터로 넘김
        if (token == null) {
            log.debug("[JwtAuthFilter] JWT 토큰 없음 - URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. 토큰 유효성 검증
            if (jwtUtil.isValid(token)) {
                // 4. 토큰에서 이메일 추출
                String email = jwtUtil.getEmail(token);
                log.info("[JwtAuthFilter] JWT 인증 시작 - email: {}, URI: {}", email, request.getRequestURI());

                // 5. 사용자 정보 조회
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                // 6. 인증 객체 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // 7. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("[JwtAuthFilter] JWT 인증 성공 - email: {}", email);
            } else {
                log.warn("[JwtAuthFilter] JWT 토큰 검증 실패 - URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("[JwtAuthFilter] JWT 인증 중 오류 발생 - URI: {}, error: {}",
                    request.getRequestURI(), e.getMessage());
        }

        // 8. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (Bearer 제거)
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // "Bearer " 제거
        }

        return null;
    }
}
