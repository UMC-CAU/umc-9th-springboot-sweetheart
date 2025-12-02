package com.example.umc9th.global.auth;

import com.example.umc9th.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Security에서 사용하는 사용자 정보 클래스
 * - UserDetails 인터페이스를 구현하여 일반 로그인 인증/인가에 필요한 정보 제공
 * - OAuth2User 인터페이스를 구현하여 OAuth2 로그인 지원
 */
@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Member member;
    private Map<String, Object> attributes; // OAuth2 로그인 시 사용하는 사용자 속성

    // 일반 로그인용 생성자
    public CustomUserDetails(Member member) {
        this.member = member;
    }

    // OAuth2 로그인용 생성자
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    /**
     * 사용자의 권한 목록 반환
     * - ROLE_USER 또는 ROLE_ADMIN
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> member.getRole().toString());
    }

    /**
     * 비밀번호 반환 (암호화된 상태)
     */
    @Override
    public String getPassword() {
        return member.getPassword();
    }

    /**
     * 사용자 식별자 반환 (이메일을 아이디로 사용)
     */
    @Override
    public String getUsername() {
        return member.getEmail();
    }

    /**
     * 계정 만료 여부 (true: 만료 안 됨)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부 (true: 잠금 안 됨)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 비밀번호 만료 여부 (true: 만료 안 됨)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부 (true: 활성화)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Member 엔티티 반환 (추가 정보가 필요할 때 사용)
     */
    public Member getMember() {
        return member;
    }

    /**
     * 회원 ID 반환 (편의 메서드)
     */
    public Long getMemberId() {
        return member.getId();
    }

    // ===== OAuth2User 인터페이스 메서드 =====

    /**
     * OAuth2 로그인 시 사용자 속성 반환
     * - Google에서 받은 사용자 정보 (email, name, picture 등)
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * OAuth2 로그인 시 사용자 이름 반환
     * - 기본적으로 이메일을 반환
     */
    @Override
    public String getName() {
        return member.getEmail();
    }
}
