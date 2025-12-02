package com.example.umc9th.global.auth.service;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.global.auth.dto.TokenDto;
import com.example.umc9th.global.auth.entity.RefreshToken;
import com.example.umc9th.global.auth.jwt.JwtUtil;
import com.example.umc9th.global.auth.repository.RefreshTokenRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 관련 서비스
 * - Token Refresh
 * - 로그아웃
 * - Refresh Token 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Access Token 재발급
     * - Refresh Token으로 새로운 Access Token 발급
     * - Refresh Token Rotation: 새 Refresh Token도 함께 발급 (보안 강화)
     *
     * @param refreshTokenString Refresh Token
     * @return 새 Access Token + 새 Refresh Token
     */
    @Transactional
    public TokenDto.TokenResponse refreshAccessToken(String refreshTokenString) {
        log.info("[AuthService.refreshAccessToken] Token Refresh 시작");

        // 1. Refresh Token 유효성 검증 (JWT 서명 + 만료 시간)
        if (!jwtUtil.isValid(refreshTokenString)) {
            log.warn("[AuthService.refreshAccessToken] 유효하지 않은 Refresh Token");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> {
                    log.warn("[AuthService.refreshAccessToken] DB에 없는 Refresh Token (로그아웃됨)");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        // 3. 만료 여부 확인
        if (refreshToken.isExpired()) {
            log.warn("[AuthService.refreshAccessToken] 만료된 Refresh Token");
            refreshTokenRepository.delete(refreshToken); // 만료된 토큰 삭제
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        // 4. 사용자 이메일 추출
        String email = jwtUtil.getEmail(refreshTokenString);
        Member member = refreshToken.getMember();

        log.info("[AuthService.refreshAccessToken] Token Refresh 성공 - email: {}, memberId: {}",
                email, member.getId());

        // 5. 새 Access Token 발급
        String newAccessToken = jwtUtil.generateAccessToken(email);

        // 6. Refresh Token Rotation (선택): 새 Refresh Token도 발급
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        // 기존 Refresh Token 업데이트
        refreshToken.updateToken(newRefreshToken, jwtUtil.getRefreshTokenExpiryDate());
        refreshTokenRepository.save(refreshToken);

        log.info("[AuthService.refreshAccessToken] 새 토큰 발급 완료 - email: {}", email);

        return TokenDto.TokenResponse.of(newAccessToken, newRefreshToken);
    }

    /**
     * Refresh Token 저장
     * - 로그인 시 호출
     *
     * @param member       회원
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void saveRefreshToken(Member member, String refreshToken) {
        log.info("[AuthService.saveRefreshToken] Refresh Token 저장 - memberId: {}", member.getId());

        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .member(member)
                .issuedAt(LocalDateTime.now())
                .expiresAt(jwtUtil.getRefreshTokenExpiryDate())
                .build();

        refreshTokenRepository.save(token);
    }

    /**
     * 로그아웃
     * - DB에서 Refresh Token 삭제
     * - Access Token은 클라이언트에서 삭제 (Stateless)
     *
     * @param refreshTokenString Refresh Token
     */
    @Transactional
    public void logout(String refreshTokenString) {
        log.info("[AuthService.logout] 로그아웃 시작");

        // Refresh Token 삭제
        refreshTokenRepository.deleteByToken(refreshTokenString);

        log.info("[AuthService.logout] 로그아웃 완료 - Refresh Token 삭제됨");
    }

    /**
     * 모든 기기에서 로그아웃
     * - 해당 회원의 모든 Refresh Token 삭제
     *
     * @param memberId 회원 ID
     */
    @Transactional
    public void logoutAllDevices(Long memberId) {
        log.info("[AuthService.logoutAllDevices] 모든 기기 로그아웃 - memberId: {}", memberId);

        refreshTokenRepository.deleteAllByMemberId(memberId);

        log.info("[AuthService.logoutAllDevices] 모든 Refresh Token 삭제 완료");
    }
}
