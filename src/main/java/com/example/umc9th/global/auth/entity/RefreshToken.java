package com.example.umc9th.global.auth.entity;

import com.example.umc9th.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.NonNull;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티
 * - MySQL에 저장하여 토큰 무효화 및 관리
 * - 로그아웃, 강제 로그아웃, 재사용 공격 탐지에 사용
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_member_id", columnList = "member_id"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Refresh Token 문자열
     * - JWT 형식
     * - 고유값으로 인덱스 설정
     */
    @NonNull
    @Column(name = "token", nullable = false, length = 512, unique = true)
    private String token;

    /**
     * 토큰 소유자
     * - Member와 N:1 관계
     * - 한 사용자가 여러 기기에서 로그인 가능
     */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 기기 정보 (선택)
     * - User-Agent 또는 기기 식별자
     * - "모든 기기에서 로그아웃" 기능에 사용
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * 발급 시간
     */
    @NonNull
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * 만료 시간
     * - 7일 후 자동 만료
     * - 배치 작업으로 만료된 토큰 정리
     */
    @NonNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰 유효성 검증
     * - 만료 여부만 확인 (JWT 서명 검증은 JwtUtil에서)
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * 토큰 정보 업데이트 (Refresh Token Rotation)
     * - 기존 토큰을 새 토큰으로 교체
     */
    public void updateToken(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
        this.issuedAt = LocalDateTime.now();
    }
}
