# Cloudflare 터널 설정 가이드

## 목표

`https://spring-swagger-api.log8.kr` 서브도메인으로 맥미니의 Spring Boot API를 HTTPS로 안전하게 노출

## 목차

1. [Cloudflare Tunnel 개요](#1-cloudflare-tunnel-개요)
2. [Cloudflare 웹 설정](#2-cloudflare-웹-설정)
3. [맥미니에서 cloudflared 설치](#3-맥미니에서-cloudflared-설치)
4. [터널 생성 및 연결](#4-터널-생성-및-연결)
5. [서브도메인 라우팅 설정](#5-서브도메인-라우팅-설정)
6. [HTTPS 접속 테스트](#6-https-접속-테스트)
7. [자동 시작 설정](#7-자동-시작-설정)

---

## 1. Cloudflare Tunnel 개요

### 1.1 왜 Cloudflare Tunnel을 사용하는가?

**기존 방식 (포트 포워딩)의 문제점:**
- ❌ 공유기 설정 필요
- ❌ 공인 IP 노출
- ❌ HTTP만 가능 (HTTPS는 인증서 설정 복잡)
- ❌ DDoS 공격 취약

**Cloudflare Tunnel의 장점:**
- ✅ **포트 포워딩 불필요**: 방화벽 뚫을 필요 없음
- ✅ **자동 HTTPS**: Cloudflare가 SSL 인증서 자동 관리
- ✅ **보안**: DDoS 보호, IP 숨김
- ✅ **서브도메인 쉽게 관리**: DNS 설정 자동화
- ✅ **무료**: 개인 프로젝트는 무료!

### 1.2 작동 원리

```
[맥미니:8080]
    ↓ (Outbound 연결)
[Cloudflare Tunnel (cloudflared)]
    ↓
[Cloudflare Edge Network]
    ↓ (HTTPS)
[사용자 브라우저] → https://spring-swagger-api.log8.kr
```

- 맥미니에서 **Cloudflare로 나가는(outbound) 연결**만 필요
- 외부에서 맥미니로 들어오는(inbound) 포트 개방 불필요
- Cloudflare가 중간에서 HTTPS 처리

---

## 2. Cloudflare 웹 설정

### 2.1 Cloudflare 대시보드 접속

1. https://dash.cloudflare.com 접속
2. **log8.kr** 도메인 선택
3. 좌측 메뉴에서 **Zero Trust** 클릭
   - 없다면: **Add a site** → Zero Trust 활성화 (무료)

### 2.2 Zero Trust 계정 생성 (처음 한 번만)

1. **Zero Trust** → **Access** → **Tunnels** 클릭
2. **Create a tunnel** 버튼 클릭
3. 터널 이름 입력: `mac-mini-umc`
4. **Save tunnel** 클릭

### 2.3 cloudflared 토큰 복사

터널 생성 후 화면에 표시되는 명령어:

```bash
# 이런 형태의 명령어가 표시됨 (토큰 포함)
cloudflared service install <YOUR_TOKEN_HERE>
```

**이 토큰을 복사해두세요!** (나중에 맥미니에서 사용)

**주의:** 브라우저를 닫으면 토큰을 다시 볼 수 없으니 반드시 저장!

---

## 3. 맥미니에서 cloudflared 설치

### 3.1 cloudflared CLI 설치

**맥미니 터미널에서:**

```bash
# Homebrew로 cloudflared 설치
brew install cloudflare/cloudflare/cloudflared

# 설치 확인
cloudflared --version
# 출력: cloudflared version 2024.x.x
```

### 3.2 Cloudflare 로그인

```bash
# Cloudflare 계정 인증
cloudflared tunnel login
```

브라우저가 자동으로 열리고 Cloudflare 로그인 페이지가 나타남:

1. Cloudflare 계정으로 로그인
2. **log8.kr** 도메인 선택
3. **Authorize** 클릭

터미널에 다음 메시지 표시:
```
You have successfully logged in.
```

인증 파일 저장 위치:
```
~/.cloudflared/cert.pem
```

---

## 4. 터널 생성 및 연결

### 4.1 터널 생성 (CLI 방식)

**옵션 A: 웹에서 이미 생성했다면 스킵**

**옵션 B: CLI로 직접 생성**

```bash
# 터널 생성
cloudflared tunnel create mac-mini-umc

# 생성된 터널 확인
cloudflared tunnel list
```

출력 예시:
```
ID                                   NAME            CREATED
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx mac-mini-umc    2024-01-15T12:00:00Z
```

**터널 ID를 복사해두세요!**

### 4.2 터널 설정 파일 생성

```bash
# 설정 디렉토리 생성
mkdir -p ~/.cloudflared

# 설정 파일 생성
nano ~/.cloudflared/config.yml
```

다음 내용 입력:

```yaml
tunnel: mac-mini-umc
credentials-file: /Users/your-username/.cloudflared/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json

# 라우팅 규칙
ingress:
  # Spring Boot API (Swagger 포함)
  - hostname: spring-swagger-api.log8.kr
    service: http://localhost:8080

  # 다른 서브도메인 추가 가능 (예시)
  # - hostname: api.log8.kr
  #   service: http://localhost:8080

  # 기본 규칙 (필수, 맨 마지막에 위치)
  - service: http_status:404
```

**중요:**
- `your-username`을 실제 맥미니 사용자명으로 변경
- `xxxxxxxx...json`을 실제 생성된 파일명으로 변경
  ```bash
  # 파일명 확인
  ls ~/.cloudflared/*.json
  ```

### 4.3 터널 DNS 라우팅 설정

```bash
# 서브도메인을 터널에 연결
cloudflared tunnel route dns mac-mini-umc spring-swagger-api.log8.kr
```

출력:
```
Added CNAME spring-swagger-api.log8.kr which will route to this tunnel.
```

이 명령어가 자동으로 수행하는 작업:
- Cloudflare DNS에 CNAME 레코드 생성
- `spring-swagger-api.log8.kr` → 터널로 라우팅

---

## 5. 서브도메인 라우팅 설정

### 5.1 Cloudflare 대시보드에서 DNS 확인

1. Cloudflare 대시보드 → **log8.kr** 도메인 선택
2. **DNS** → **Records** 클릭
3. 다음 레코드가 자동으로 추가되었는지 확인:

   | Type | Name | Content | Proxy status |
   |------|------|---------|--------------|
   | CNAME | spring-swagger-api | xxxxxxxx.cfargotunnel.com | Proxied (오렌지 구름) |

**주의:** Proxy status가 **Proxied (오렌지 구름)**인지 확인!

### 5.2 수동으로 DNS 레코드 추가 (자동 생성 실패 시)

CLI 명령어가 실패했다면 수동으로 추가:

1. Cloudflare DNS 페이지에서 **Add record** 클릭
2. 다음 정보 입력:
   ```
   Type: CNAME
   Name: spring-swagger-api
   Target: <TUNNEL_ID>.cfargotunnel.com
   Proxy status: Proxied (오렌지 구름 활성화)
   TTL: Auto
   ```
3. **Save** 클릭

**터널 ID 확인 방법:**
```bash
cloudflared tunnel list
```

---

## 6. HTTPS 접속 테스트

### 6.1 터널 실행 (테스트)

**맥미니 터미널에서:**

```bash
# Spring Boot 앱이 실행 중인지 확인
curl http://localhost:8080/actuator/health

# 터널 실행 (포그라운드 모드, 테스트용)
cloudflared tunnel run mac-mini-umc
```

출력:
```
2024-01-15T12:00:00Z INF Starting tunnel tunnelID=xxxxxxxx
2024-01-15T12:00:01Z INF Connection established connIndex=0
2024-01-15T12:00:01Z INF Registered tunnel connection connIndex=0
```

**새 터미널 창에서 테스트:**

```bash
# 외부에서 접속 테스트
curl https://spring-swagger-api.log8.kr/actuator/health
```

출력:
```json
{"status":"UP"}
```

### 6.2 브라우저에서 접속

1. 브라우저 열기
2. 주소창에 입력: `https://spring-swagger-api.log8.kr`
3. ✅ HTTPS 인증서가 자동으로 적용되어 있음 (자물쇠 아이콘)
4. Swagger UI 확인: `https://spring-swagger-api.log8.kr/swagger-ui.html`

### 6.3 Windows 데스크톱에서 테스트

```bash
# Windows 터미널에서
curl https://spring-swagger-api.log8.kr/actuator/health
```

또는 브라우저에서 동일하게 접속 가능!

---

## 7. 자동 시작 설정

### 7.1 터널을 서비스로 등록 (추천)

**맥미니 터미널에서:**

```bash
# cloudflared를 시스템 서비스로 설치
sudo cloudflared service install

# 서비스 시작
sudo launchctl start com.cloudflare.cloudflared

# 서비스 상태 확인
sudo launchctl list | grep cloudflare
```

**장점:**
- 맥미니 재부팅 시 자동 시작
- 백그라운드에서 항상 실행
- 로그 자동 관리

### 7.2 서비스 로그 확인

```bash
# 로그 실시간 확인
sudo tail -f /var/log/cloudflared.log

# 또는
sudo cloudflared service logs
```

### 7.3 서비스 관리 명령어

```bash
# 서비스 중지
sudo launchctl stop com.cloudflare.cloudflared

# 서비스 재시작
sudo launchctl stop com.cloudflare.cloudflared
sudo launchctl start com.cloudflare.cloudflared

# 서비스 제거 (필요시)
sudo cloudflared service uninstall
```

---

## 8. Cloudflare 웹 대시보드에서 확인

### 8.1 터널 상태 확인

1. Cloudflare Zero Trust 대시보드
2. **Access** → **Tunnels**
3. `mac-mini-umc` 터널 상태 확인:
   - **Status**: Healthy (초록색)
   - **Connections**: Active

### 8.2 트래픽 모니터링

1. **Analytics** → **Traffic** 클릭
2. `spring-swagger-api.log8.kr` 서브도메인의 요청 확인 가능

---

## 9. 고급 설정 (선택 사항)

### 9.1 Access Policy 설정 (인증 추가)

Spring Boot API를 공개하지 않고 인증된 사용자만 접근하도록 설정:

1. Cloudflare Zero Trust → **Access** → **Applications**
2. **Add an application** 클릭
3. **Self-hosted** 선택
4. 다음 정보 입력:
   ```
   Application name: UMC Spring API
   Subdomain: spring-swagger-api
   Domain: log8.kr
   ```
5. **Access Policy** 추가:
   ```
   Rule name: Allow specific emails
   Action: Allow
   Rule: Emails ending in @your-domain.com
   ```
6. **Save application**

이제 `https://spring-swagger-api.log8.kr` 접속 시 Cloudflare 로그인 페이지가 나타남!

### 9.2 여러 서비스 라우팅

config.yml 수정:

```yaml
tunnel: mac-mini-umc
credentials-file: /Users/your-username/.cloudflared/xxx.json

ingress:
  # Swagger API
  - hostname: spring-swagger-api.log8.kr
    service: http://localhost:8080

  # 프론트엔드 (나중에 추가)
  - hostname: app.log8.kr
    service: http://localhost:3000

  # MySQL Admin (phpMyAdmin 등)
  - hostname: db-admin.log8.kr
    service: http://localhost:8081

  # 기본 규칙
  - service: http_status:404
```

각 서브도메인을 DNS에 등록:

```bash
cloudflared tunnel route dns mac-mini-umc spring-swagger-api.log8.kr
cloudflared tunnel route dns mac-mini-umc app.log8.kr
cloudflared tunnel route dns mac-mini-umc db-admin.log8.kr
```

### 9.3 로드 밸런싱 (여러 맥미니/서버)

config.yml:

```yaml
ingress:
  - hostname: spring-swagger-api.log8.kr
    service: http_status:200
    originRequest:
      connectTimeout: 10s
      noTLSVerify: false
```

---

## 10. 트러블슈팅

### 10.1 터널이 연결되지 않음

```bash
# 터널 상태 확인
cloudflared tunnel info mac-mini-umc

# 설정 파일 검증
cloudflared tunnel ingress validate

# 터널 재시작
sudo launchctl stop com.cloudflare.cloudflared
sudo launchctl start com.cloudflare.cloudflared
```

### 10.2 DNS 전파 확인

```bash
# DNS 조회
nslookup spring-swagger-api.log8.kr

# 또는
dig spring-swagger-api.log8.kr
```

출력에 CNAME 레코드가 보여야 함:
```
spring-swagger-api.log8.kr. 300 IN CNAME xxxxxxxx.cfargotunnel.com.
```

### 10.3 502 Bad Gateway 에러

**원인:**
- Spring Boot 앱이 실행되지 않음
- 포트 번호 불일치

**해결:**

```bash
# Spring Boot 앱 상태 확인
docker compose ps

# 로컬에서 접근 테스트
curl http://localhost:8080/actuator/health

# config.yml의 포트 번호 확인
cat ~/.cloudflared/config.yml
```

### 10.4 HTTPS 인증서 오류

**원인:**
- Cloudflare Proxy가 비활성화됨

**해결:**
1. Cloudflare DNS 페이지
2. `spring-swagger-api` CNAME 레코드의 Proxy status 확인
3. **Proxied (오렌지 구름)** 활성화

---

## 11. 완료 체크리스트

- [ ] Cloudflare Zero Trust 계정 생성
- [ ] cloudflared CLI 설치 (맥미니)
- [ ] Cloudflare 로그인 (`cloudflared tunnel login`)
- [ ] 터널 생성 (`mac-mini-umc`)
- [ ] config.yml 작성
- [ ] DNS 라우팅 설정 (`spring-swagger-api.log8.kr`)
- [ ] 터널 실행 및 테스트
- [ ] HTTPS 접속 성공
- [ ] 서비스 자동 시작 설정
- [ ] Windows에서 접속 확인

---

## 12. 최종 확인

### 12.1 모든 엔드포인트 테스트

```bash
# Health check
curl https://spring-swagger-api.log8.kr/actuator/health

# Swagger UI (브라우저에서)
https://spring-swagger-api.log8.kr/swagger-ui.html

# API 호출 (예시)
curl https://spring-swagger-api.log8.kr/api/members
```

### 12.2 성능 확인

```bash
# 응답 시간 측정
time curl https://spring-swagger-api.log8.kr/actuator/health
```

보통 100-300ms 정도 (Cloudflare 경유)

---

## 다음 단계

✅ Cloudflare 터널 설정 완료!

다음 문서를 참고하세요:
- `CICD_SETUP.md`: GitHub Actions로 자동 배포 설정
- `DEPLOYMENT.md`: 전체 배포 워크플로우
