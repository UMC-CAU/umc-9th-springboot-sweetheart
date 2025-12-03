package com.example.umc9th.global.auth.oauth2;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.auth.CustomUserDetails;
import com.example.umc9th.global.auth.enums.Role;
import com.example.umc9th.global.auth.enums.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 * - Google에서 받은 사용자 정보를 DB에 저장/조회
 * - 최초 로그인 시 자동으로 회원가입 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    /**
     * OAuth2 로그인 성공 후 호출되는 메서드
     * - Google에서 받은 Access Token으로 사용자 정보 가져오기
     * - DB에 회원 저장 또는 조회
     *
     * @param userRequest OAuth2 로그인 요청 정보 (Access Token 포함)
     * @return OAuth2User 사용자 정보
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("[CustomOAuth2UserService.loadUser] OAuth2 로그인 시작");

        // 부모 클래스의 loadUser로 Google 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Google에서 받은 사용자 정보 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        log.info("[CustomOAuth2UserService.loadUser] Provider: {}, Attributes: {}", registrationId, attributes);

        // Google 로그인 처리
        if ("google".equals(registrationId)) {
            return processGoogleUser(attributes);
        }

        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 Provider입니다: " + registrationId);
    }

    /**
     * Google 사용자 정보 처리
     * - 이메일로 기존 회원 조회
     * - 없으면 자동 회원가입 (소셜 로그인)
     *
     * @param attributes Google에서 받은 사용자 정보
     * @return CustomUserDetails 우리 서비스의 사용자 정보
     */
    private OAuth2User processGoogleUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture"); // 프로필 이미지 URL

        log.info("[CustomOAuth2UserService.processGoogleUser] Google 사용자 - email: {}, name: {}, picture: {}",
                email, name, picture);

        // 이메일로 기존 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("[CustomOAuth2UserService.processGoogleUser] 신규 회원 자동 가입 - email: {}", email);
                    // 최초 로그인: 자동 회원가입
                    Member newMember = Member.builder()
                            .email(email)
                            .name(name)
                            .password("") // 소셜 로그인은 비밀번호 불필요
                            .gender(Gender.NONE) // 기본값
                            .role(Role.ROLE_USER) // 일반 사용자 권한
                            .birth(java.time.LocalDate.of(2000, 1, 1)) // 기본 생년월일
                            .address("미설정") // 기본 주소
                            .detailAddress("미설정") // 기본 상세 주소
                            .socialUid(email) // Google 이메일을 소셜 UID로 사용
                            .socialType(SocialType.GOOGLE) // Google 로그인
                            .point(0) // 초기 포인트 0
                            .build();
                    return memberRepository.save(newMember);
                });

        log.info("[CustomOAuth2UserService.processGoogleUser] 회원 조회/생성 완료 - ID: {}, email: {}",
                member.getId(), member.getEmail());

        // CustomUserDetails로 변환하여 반환 (OAuth2 속성 포함)
        return new CustomUserDetails(member, attributes);
    }
}
