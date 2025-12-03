package com.example.umc9th.global.auth.jwt;

import com.example.umc9th.global.auth.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT 토큰 생성/검증 유틸리티 클래스
 * - Access Token 생성
 * - 토큰에서 사용자 정보 추출
 * - 토큰 유효성 검증
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public JwtUtil(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.access-token-expiration}") Long accessExpiration,
            @Value("${jwt.refresh-token-expiration}") Long refreshExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
        this.refreshExpiration = Duration.ofMillis(refreshExpiration);
        log.info("[JwtUtil] JWT 설정 초기화 - Access 만료: {}시간, Refresh 만료: {}일",
                accessExpiration / 3600000, refreshExpiration / 86400000);
    }

    /**
     * Access Token 생성
     *
     * @param user 사용자 정보
     * @return JWT Access Token
     */
    public String createAccessToken(CustomUserDetails user) {
        log.info("[JwtUtil.createAccessToken] Access Token 생성 - email: {}", user.getUsername());
        return createToken(user, accessExpiration);
    }

    /**
     * Refresh Token 생성
     *
     * @param user 사용자 정보
     * @return JWT Refresh Token
     */
    public String createRefreshToken(CustomUserDetails user) {
        log.info("[JwtUtil.createRefreshToken] Refresh Token 생성 - email: {}", user.getUsername());
        return createToken(user, refreshExpiration);
    }

    /**
     * Access Token 생성 (이메일만으로)
     * - Token Refresh 시 사용
     * - OAuth2 로그인 성공 핸들러에서 사용
     *
     * @param email 사용자 이메일
     * @return JWT Access Token
     */
    public String generateAccessToken(String email) {
        log.info("[JwtUtil.generateAccessToken] Access Token 생성 (이메일만) - email: {}", email);
        return createSimpleToken(email, accessExpiration);
    }

    /**
     * Refresh Token 생성 (이메일만으로)
     * - Token Refresh 시 사용
     *
     * @param email 사용자 이메일
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String email) {
        log.info("[JwtUtil.generateRefreshToken] Refresh Token 생성 (이메일만) - email: {}", email);
        return createSimpleToken(email, refreshExpiration);
    }

    /**
     * 토큰에서 이메일 추출
     *
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmail(String token) {
        try {
            String email = getClaims(token).getPayload().getSubject();
            log.debug("[JwtUtil.getEmail] 토큰에서 이메일 추출 성공 - email: {}", email);
            return email;
        } catch (JwtException e) {
            log.warn("[JwtUtil.getEmail] 토큰에서 이메일 추출 실패 - error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 유효성 검증
     *
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            log.debug("[JwtUtil.isValid] 토큰 검증 성공");
            return true;
        } catch (JwtException e) {
            log.warn("[JwtUtil.isValid] 토큰 검증 실패 - error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰 생성 (공통 로직)
     *
     * @param user       사용자 정보
     * @param expiration 만료 시간
     * @return JWT 토큰
     */
    private String createToken(CustomUserDetails user, Duration expiration) {
        Instant now = Instant.now();

        // 사용자 권한 정보
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String token = Jwts.builder()
                .subject(user.getUsername())  // 이메일을 Subject로 설정
                .claim("role", authorities)   // 권한 정보
                .claim("email", user.getUsername())  // 이메일
                .claim("memberId", user.getMember().getId())  // 회원 ID
                .issuedAt(Date.from(now))  // 발급 시간
                .expiration(Date.from(now.plus(expiration)))  // 만료 시간
                .signWith(secretKey)  // 서명
                .compact();

        log.info("[JwtUtil.createToken] 토큰 생성 완료 - email: {}, 만료: {}초 후",
                user.getUsername(), expiration.getSeconds());

        return token;
    }

    /**
     * JWT 토큰 생성 (이메일만으로)
     * - Refresh Token으로 새 Access Token 발급 시 사용
     * - CustomUserDetails 없이 간단하게 토큰 생성
     *
     * @param email      사용자 이메일
     * @param expiration 만료 시간
     * @return JWT 토큰
     */
    private String createSimpleToken(String email, Duration expiration) {
        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(email)  // 이메일을 Subject로 설정
                .claim("email", email)  // 이메일
                .issuedAt(Date.from(now))  // 발급 시간
                .expiration(Date.from(now.plus(expiration)))  // 만료 시간
                .signWith(secretKey)  // 서명
                .compact();

        log.info("[JwtUtil.createSimpleToken] 간단한 토큰 생성 완료 - email: {}, 만료: {}초 후",
                email, expiration.getSeconds());

        return token;
    }

    /**
     * Refresh Token 만료 시간 조회 (LocalDateTime)
     * - RefreshToken 엔티티 저장 시 사용
     */
    public java.time.LocalDateTime getRefreshTokenExpiryDate() {
        return java.time.LocalDateTime.now().plus(refreshExpiration);
    }

    /**
     * 토큰 파싱 및 Claims 추출
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰이 유효하지 않을 때
     */
    private Jws<Claims> getClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(60)  // 시간 오차 허용 (60초)
                .build()
                .parseSignedClaims(token);
    }
}
