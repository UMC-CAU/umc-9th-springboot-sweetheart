package com.example.umc9th.global.auth.repository;

import com.example.umc9th.global.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Repository
 * - MySQL에 저장된 Refresh Token 관리
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 문자열로 조회
     * - Token Refresh API에서 사용
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 회원 ID로 모든 토큰 조회
     * - "모든 기기에서 로그아웃" 기능에 사용
     */
    List<RefreshToken> findAllByMemberId(Long memberId);

    /**
     * 회원 ID로 모든 토큰 삭제
     * - "모든 기기에서 로그아웃" 기능에 사용
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원 이메일로 모든 토큰 삭제
     * - 로그아웃 시 사용
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.member.email = :email")
    void deleteAllByMemberEmail(@Param("email") String email);

    /**
     * 특정 토큰 삭제
     * - 단일 기기 로그아웃에 사용
     */
    void deleteByToken(String token);

    /**
     * 만료된 토큰 삭제
     * - 배치 작업으로 주기적으로 실행
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 회원의 토큰 개수 조회
     * - 동시 로그인 기기 수 제한에 사용
     */
    long countByMemberId(Long memberId);

    /**
     * 토큰 존재 여부 확인
     */
    boolean existsByToken(String token);
}
