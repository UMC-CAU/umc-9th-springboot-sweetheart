package com.example.umc9th.global.auth.controller;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.repository.MemberRepository;
import com.example.umc9th.global.auth.dto.TokenDto;
import com.example.umc9th.global.auth.jwt.JwtUtil;
import com.example.umc9th.global.auth.service.AuthService;
import com.example.umc9th.global.exception.CustomException;
import com.example.umc9th.global.response.ApiResponse;
import com.example.umc9th.global.response.code.ErrorCode;
import com.example.umc9th.global.response.code.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * - Token Refresh
 * - 로그아웃
 */
@Tag(name = "Auth", description = "인증 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Operation(
            summary = "Access Token 재발급",
            description = """
                    Refresh Token으로 새로운 Access Token을 발급받습니다.

                    **Refresh Token Rotation 적용:**
                    - 보안 강화를 위해 새 Refresh Token도 함께 발급됩니다
                    - 기존 Refresh Token은 무효화됩니다

                    **사용 시나리오:**
                    1. Access Token 만료 시 (401 Unauthorized)
                    2. 클라이언트가 Refresh Token으로 재발급 요청
                    3. 새 Access Token + 새 Refresh Token 수신
                    4. 기존 토큰들 폐기, 새 토큰들로 교체
                    """
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenDto.TokenResponse> refreshToken(
            @Valid @RequestBody TokenDto.RefreshRequest request
    ) {
        log.info("[AuthController.refreshToken] Token Refresh 요청");

        TokenDto.TokenResponse response = authService.refreshAccessToken(request.refreshToken());

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    현재 기기에서 로그아웃합니다.

                    **처리 과정:**
                    1. DB에서 Refresh Token 삭제
                    2. 클라이언트는 Access Token + Refresh Token 모두 삭제
                    3. 현재 Access Token은 만료될 때까지 유효 (최대 15분)

                    **주의:**
                    - Access Token은 Stateless이므로 서버에서 즉시 무효화 불가
                    - 하지만 Refresh Token이 없으면 재발급 불가능
                    - 따라서 최대 15분 후 완전히 로그아웃됨
                    """
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody TokenDto.LogoutRequest request
    ) {
        log.info("[AuthController.logout] 로그아웃 요청");

        authService.logout(request.refreshToken());

        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @Operation(
            summary = "모든 기기에서 로그아웃",
            description = """
                    해당 회원의 모든 Refresh Token을 삭제하여 모든 기기에서 로그아웃합니다.

                    **사용 시나리오:**
                    - 계정 해킹 의심 시
                    - 비밀번호 변경 후
                    - 보안 강화가 필요한 경우

                    **Authorization 헤더 필요:**
                    - Bearer {Access Token}
                    """
    )
    @PostMapping("/logout/all")
    public ApiResponse<Void> logoutAllDevices(
            @RequestHeader("Authorization") String authHeader
    ) {
        log.info("[AuthController.logoutAllDevices] 모든 기기 로그아웃 요청");

        // Authorization 헤더에서 토큰 추출
        String token = authHeader.replace("Bearer ", "");

        // Access Token 유효성 검증
        if (!jwtUtil.isValid(token)) {
            log.warn("[AuthController.logoutAllDevices] 유효하지 않은 Access Token");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰에서 이메일 추출
        String email = jwtUtil.getEmail(token);

        // 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AuthController.logoutAllDevices] 회원을 찾을 수 없음 - email: {}", email);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        // 모든 기기에서 로그아웃 (모든 Refresh Token 삭제)
        authService.logoutAllDevices(member.getId());

        log.info("[AuthController.logoutAllDevices] 모든 기기 로그아웃 완료 - memberId: {}", member.getId());

        return ApiResponse.onSuccess(SuccessCode.OK);
    }
}
