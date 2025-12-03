# Access Token vs Refresh Token 비교 분석 문서

**작성자**: UMC 9기 Sweetheart
**작성일**: 2025-01-15
**프로젝트**: Spring Boot JWT 인증 시스템

---

## 📋 목차

1. [개요](#개요)
2. [Access Token만 사용 (기존 방식)](#access-token만-사용-기존-방식)
3. [Refresh Token 추가 (개선 방식)](#refresh-token-추가-개선-방식)
4. [장단점 비교](#장단점-비교)
5. [보안 시나리오 비교](#보안-시나리오-비교)
6. [구현 복잡도 비교](#구현-복잡도-비교)
7. [성능 비교](#성능-비교)
8. [결론](#결론)

---

## 개요

JWT 기반 인증 시스템에서 **Access Token만 사용하는 방식**과 **Refresh Token을 추가한 방식**을 비교 분석합니다.

### 시스템 환경
- **Backend**: Spring Boot 3.5.6, MySQL 8.0
- **JWT 라이브러리**: jjwt 0.12.3
- **배포 환경**: Mac Mini (macOS), Cloudflare Tunnel

---

## Access Token만 사용 (기존 방식)

### 아키텍처

```
[로그인]
사용자 → 이메일/비밀번호 → 서버
                              ↓
                        JWT Access Token 발급
                        (유효 기간: 4시간)
                              ↓
                        클라이언트에 전달

[API 호출]
클라이언트 → Access Token → 서버
                            ↓
                       JWT 서명 검증
                       만료 시간 확인
                            ↓
                       API 처리 및 응답
```

### 특징

| 항목 | 설명 |
|------|------|
| **토큰 유효 기간** | 4시간 |
| **서버 저장** | 없음 (Stateless) |
| **토큰 갱신** | 만료 시 재로그인 필요 |
| **로그아웃** | 클라이언트에서 토큰 삭제 |

### 코드 예시

```java
// 로그인 시 Access Token만 발급
public MemberResponse.Login login(MemberRequest.Login request) {
    // 1. 이메일/비밀번호 검증
    Member member = memberRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
        throw new CustomException(ErrorCode.MEMBER_INVALID_PASSWORD);
    }

    // 2. JWT Access Token 발급
    CustomUserDetails userDetails = new CustomUserDetails(member);
    String accessToken = jwtUtil.createAccessToken(userDetails);

    return MemberResponse.Login.of(member, accessToken);
}
```

---

## Refresh Token 추가 (개선 방식)

### 아키텍처

```
[로그인]
사용자 → 이메일/비밀번호 → 서버
                              ↓
                        Access Token (15분) +
                        Refresh Token (7일) 발급
                              ↓
                        Refresh Token → MySQL 저장
                              ↓
                        둘 다 클라이언트에 전달

[API 호출]
클라이언트 → Access Token → 서버
                            ↓
                       JWT 서명 검증
                            ↓
                       API 처리 및 응답

[Access Token 만료 시]
클라이언트 → Refresh Token → 서버
                              ↓
                         DB에서 Refresh Token 확인
                              ↓
                         새 Access Token 발급
                         (선택) 새 Refresh Token 발급

[로그아웃]
클라이언트 → Refresh Token → 서버
                              ↓
                         DB에서 Refresh Token 삭제
                              ↓
                         15분 후 완전 로그아웃
```

### 특징

| 항목 | Access Token | Refresh Token |
|------|--------------|---------------|
| **유효 기간** | 15분 | 7일 |
| **서버 저장** | 없음 (Stateless) | MySQL (Stateful) |
| **용도** | API 호출 인증 | Access Token 재발급 |
| **탈취 위험** | 낮음 (짧은 수명) | 높음 (긴 수명) → DB 관리 필수 |

### 코드 예시

```java
// 로그인 시 Access Token + Refresh Token 발급
public MemberResponse.Login login(MemberRequest.Login request) {
    // 1. 이메일/비밀번호 검증 (동일)
    Member member = memberRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
        throw new CustomException(ErrorCode.MEMBER_INVALID_PASSWORD);
    }

    // 2. JWT Access Token + Refresh Token 발급
    CustomUserDetails userDetails = new CustomUserDetails(member);
    String accessToken = jwtUtil.createAccessToken(userDetails);
    String refreshToken = jwtUtil.createRefreshToken(userDetails);

    // 3. Refresh Token DB에 저장
    authService.saveRefreshToken(member, refreshToken);

    return MemberResponse.Login.of(member, accessToken, refreshToken);
}

// Token Refresh API
@PostMapping("/api/auth/refresh")
public ApiResponse<TokenDto.TokenResponse> refreshToken(@RequestBody TokenDto.RefreshRequest request) {
    TokenDto.TokenResponse response = authService.refreshAccessToken(request.refreshToken());
    return ApiResponse.onSuccess(SuccessCode.OK, response);
}
```

---

## 장단점 비교

### Access Token만 사용 (기존 방식)

#### ✅ 장점

1. **구현 간단**: 토큰 발급만 하면 끝
2. **서버 부하 없음**: Stateless, DB 조회 불필요
3. **확장성 우수**: 서버 여러 대 배포 시 Session 동기화 불필요
4. **빠른 인증**: JWT 서명 검증만으로 인증 완료

#### ❌ 단점

1. **강제 로그아웃 불가능**: 한번 발급된 토큰은 만료 전까지 유효
2. **토큰 탈취 시 위험**: 4시간 동안 계속 사용 가능
3. **사용자 불편**: 4시간마다 재로그인 필요
4. **보안 취약**: 계정 정지 시에도 기존 토큰은 유효

#### 예시 시나리오

```
[시나리오 1: 토큰 탈취]
09:00 - 사용자 로그인 (Access Token 발급, 13:00까지 유효)
10:00 - 공격자가 네트워크에서 토큰 가로채기
11:00 - 사용자가 이상 감지 후 "로그아웃" 클릭
      → 하지만 서버는 모름! (Stateless)
12:00 - 공격자가 여전히 토큰 사용 가능 ❌
13:00 - 토큰 만료되어야 비로소 사용 불가

결과: 2시간 동안 공격자가 계정 사용 가능
```

---

### Refresh Token 추가 (개선 방식)

#### ✅ 장점

1. **보안 강화**: Access Token 짧게 → 탈취 위험 최소화
2. **강제 로그아웃 가능**: DB에서 Refresh Token 삭제 → 재발급 차단
3. **사용자 편의**: 7일 동안 자동 로그인 유지
4. **재사용 공격 탐지**: Refresh Token Rotation으로 탈취 감지 가능
5. **기기별 관리**: 여러 기기 로그인 상태 추적 가능

#### ❌ 단점

1. **구현 복잡도 증가**: DB 관리, Token Refresh API 추가
2. **DB 조회 필요**: Token Refresh 시 DB 읽기/쓰기
3. **저장소 관리**: Refresh Token 테이블 관리 필요
4. **완전한 즉시 무효화 불가**: Access Token은 여전히 만료까지 유효 (최대 15분)

#### 예시 시나리오

```
[시나리오 2: 토큰 탈취 + Refresh Token]
09:00 - 사용자 로그인 (Access: 09:15까지, Refresh: 7일)
      → Refresh Token DB 저장
10:00 - 공격자가 Access Token + Refresh Token 가로채기
11:00 - 사용자가 이상 감지 후 "로그아웃" 클릭
      → 서버: DB에서 Refresh Token 삭제
11:05 - 공격자가 Access Token 사용 (여전히 유효)
11:15 - Access Token 만료
11:16 - 공격자가 Token Refresh 시도
      → 서버: "DB에 없는 Refresh Token입니다" ❌
      → 재발급 실패, 공격자 차단 성공! ✅

결과: 최대 15분 후 완전히 차단 (vs 기존 4시간)
```

---

## 보안 시나리오 비교

### 1. 계정 해킹 의심

| 상황 | Access Token만 | Refresh Token 추가 |
|------|----------------|-------------------|
| **해킹 발견** | 11:00 | 11:00 |
| **조치** | 로그아웃 (클라이언트에서 토큰 삭제) | "모든 기기에서 로그아웃" (DB에서 모든 Refresh Token 삭제) |
| **해커 차단 시점** | 13:00 (Access Token 만료) | 11:15 (Access Token 만료 + Refresh 불가) |
| **피해 시간** | **2시간** ❌ | **15분** ✅ |

### 2. 비밀번호 변경

| 상황 | Access Token만 | Refresh Token 추가 |
|------|----------------|-------------------|
| **비밀번호 변경** | 모든 기존 토큰 여전히 유효 | 모든 Refresh Token 삭제 가능 |
| **기존 세션** | 4시간 동안 유효 ❌ | 15분 후 모두 무효화 ✅ |

### 3. 계정 정지

| 상황 | Access Token만 | Refresh Token 추가 |
|------|----------------|-------------------|
| **관리자 조치** | 계정 정지 | 계정 정지 + Refresh Token 삭제 |
| **사용자 접근** | 4시간 동안 계속 가능 ❌ | 15분 후 완전 차단 ✅ |

---

## 구현 복잡도 비교

### Access Token만 (기존)

**필요한 구현**:
1. JwtUtil (토큰 생성/검증)
2. JwtAuthFilter (Authorization 헤더 검증)
3. 로그인 API (토큰 발급)

**총 파일 수**: 3개
**DB 테이블**: 0개
**API 엔드포인트**: 2개 (로그인, 회원가입)

---

### Refresh Token 추가 (개선)

**추가 구현**:
1. RefreshToken 엔티티 + Repository
2. AuthService (Token Refresh 로직)
3. AuthController (Token Refresh, 로그아웃 API)
4. TokenCleanupScheduler (만료 토큰 자동 삭제)
5. ErrorCode 추가 (INVALID_TOKEN, EXPIRED_TOKEN)
6. MemberService 수정 (Refresh Token 발급)
7. OAuth2SuccessHandler 수정

**총 추가 파일 수**: 7개
**DB 테이블**: 1개 (refresh_token)
**API 엔드포인트**: 2개 추가 (Token Refresh, 로그아웃)

**구현 시간**: 약 2~3시간

---

## 성능 비교

### API 호출 시 (Access Token 검증)

| 항목 | Access Token만 | Refresh Token 추가 |
|------|----------------|-------------------|
| **DB 조회** | 없음 | 없음 |
| **검증 시간** | ~1ms (JWT 서명 검증) | ~1ms (JWT 서명 검증) |
| **결론** | **동일** ✅ | **동일** ✅ |

→ **Access Token 검증은 둘 다 Stateless이므로 성능 동일!**

---

### Token Refresh 시

| 항목 | Access Token만 | Refresh Token 추가 |
|------|----------------|-------------------|
| **발생 빈도** | 4시간마다 (재로그인) | 15분마다 (Token Refresh) |
| **사용자 행동** | 수동 재로그인 | 자동 재발급 |
| **DB 조회** | 로그인 시 1회 | Token Refresh 시 1회 |
| **사용자 경험** | 불편 ❌ | 편리 ✅ |

**일일 API 호출 수 (사용자 1000명 기준)**:

```
[Access Token만]
- Token Refresh: 0회/일 (없음)
- 로그인: 6회/일 × 1000명 = 6,000회

[Refresh Token 추가]
- Token Refresh: 96회/일 × 1000명 = 96,000회
- 로그인: 1회/일 × 1000명 = 1,000회

DB 조회 증가: +90,000회/일
하지만: 단순 SELECT 쿼리 (~1ms)
총 시간: 90초/일 → 무시 가능! ✅
```

---

## 결론

### 권장 사항

| 프로젝트 규모 | 권장 방식 | 이유 |
|--------------|----------|------|
| **프로토타입/학습용** | Access Token만 | 간단하고 빠른 구현 |
| **실제 서비스** | **Refresh Token 추가** ✅ | 보안 + 사용자 경험 |
| **금융/의료 등 민감 서비스** | **필수: Refresh Token** | 강제 로그아웃 필수 |

### 최종 평가

| 평가 항목 | Access Token만 | Refresh Token 추가 | 승자 |
|----------|----------------|-------------------|------|
| **구현 난이도** | ⭐⭐ (쉬움) | ⭐⭐⭐⭐ (보통) | Access Token |
| **보안성** | ⭐⭐ | ⭐⭐⭐⭐⭐ | **Refresh Token** ✅ |
| **사용자 경험** | ⭐⭐ (4시간마다 재로그인) | ⭐⭐⭐⭐⭐ (7일 유지) | **Refresh Token** ✅ |
| **성능** | ⭐⭐⭐⭐⭐ (DB 없음) | ⭐⭐⭐⭐⭐ (영향 미미) | **동일** ✅ |
| **확장성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **동일** ✅ |
| **강제 로그아웃** | ❌ 불가능 | ✅ 가능 | **Refresh Token** ✅ |

**종합 점수**: Refresh Token 방식 압도적 승리! 🎉

---

## 실제 적용 결과

### UMC 9기 Spring Boot 프로젝트

- **Before**: Access Token만 사용 (4시간 유효)
- **After**: Refresh Token 추가 (Access 15분 + Refresh 7일)

**개선 효과**:
1. ✅ 보안 강화: 토큰 탈취 시 피해 시간 **2시간 → 15분** (93% 감소)
2. ✅ 사용자 편의: 재로그인 주기 **4시간 → 7일** (42배 개선)
3. ✅ 강제 로그아웃: 불가능 → 가능
4. ✅ 기기 관리: 불가능 → 가능 (여러 기기 추적)

**성능 영향**: 미미 (DB 조회 증가 but 1ms 수준)

---

## 참고 자료

- [RFC 6749 - OAuth 2.0 Authorization Framework](https://datatracker.ietf.org/doc/html/rfc6749)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP Token Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

---

**작성 완료일**: 2025-01-15
**버전**: 1.0
