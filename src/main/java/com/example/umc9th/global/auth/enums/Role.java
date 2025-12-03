package com.example.umc9th.global.auth.enums;

/**
 * 사용자 권한 Enum
 * - ROLE_USER: 일반 사용자
 * - ROLE_ADMIN: 관리자
 *
 * 주의: Spring Security의 hasRole() 메서드 사용 시
 * DB에 "ROLE_" 접두사가 포함되어 있어야 합니다.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
