package com.example.umc9th.global.auth;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security에서 사용자 정보를 조회하는 서비스
 * - 로그인 시 AuthenticationManager가 이 서비스를 호출하여 사용자 정보를 가져옴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 사용자 이름(이메일)으로 사용자 정보 조회
     * - Spring Security가 로그인 시 자동으로 호출
     *
     * @param username 사용자 이메일
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[CustomUserDetailsService.loadUserByUsername] 사용자 조회 - email: {}", username);

        // 이메일로 회원 조회
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.warn("[CustomUserDetailsService.loadUserByUsername] 사용자를 찾을 수 없음 - email: {}", username);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        log.info("[CustomUserDetailsService.loadUserByUsername] 사용자 조회 성공 - ID: {}, email: {}",
                member.getId(), member.getEmail());

        // CustomUserDetails로 변환하여 반환
        return new CustomUserDetails(member);
    }
}
