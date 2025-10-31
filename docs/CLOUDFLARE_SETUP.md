# Cloudflare Tunnel 설정 가이드

## 목표

HTTPS로 외부에서 Spring Boot API에 안전하게 접속하기

**완성 후:**
- ✅ 외부에서 `https://[your-subdomain].[your-domain]`로 접속 가능
- ✅ 자동 HTTPS (SSL 인증서 자동)
- ✅ 포트 포워딩 불필요
- ✅ DDoS 보호

**⚠️ 참고:** 이 설정은 **외부 사용자 접속용**입니다. GitHub Actions 자동 배포와는 별개입니다.

---

## 1. cloudflared 설치

### 1.1 맥미니에 설치

**SSH로 맥미니 접속:**
```bash
ssh [your_username]@[your_mac_mini_ip]
```

**cloudflared 설치:**
```bash
# Homebrew로 설치
brew install cloudflare/cloudflare/cloudflared

# 확인
cloudflared --version
```

### 1.2 Cloudflare 로그인

```bash
cloudflared tunnel login
```

브라우저가 열리면:
1. Cloudflare 계정으로 로그인
2. 도메인 선택 (예: `[your-domain]`)
3. **Authorize** 클릭

성공 메시지:
```
You have successfully logged in.
```

---

## 2. 터널 생성

### 2.1 터널 생성

```bash
# 터널 생성
cloudflared tunnel create [tunnel-name]

# 생성 확인
cloudflared tunnel list
```

**출력 예시:**
```
ID                                   NAME           CREATED
[tunnel-id]                          [tunnel-name]  2024-01-15T12:00:00Z
```

**터널 ID를 복사해두세요!**

---

## 3. 터널 설정

### 3.1 시스템 레벨 설정 디렉토리 생성

```bash
# 설정 디렉토리 생성
sudo mkdir -p /etc/cloudflared

# credentials 파일 복사
sudo cp ~/.cloudflared/[tunnel-id].json /etc/cloudflared/
```

### 3.2 설정 파일 생성

```bash
sudo nano /etc/cloudflared/config.yml
```

**내용 입력:**
```yaml
tunnel: [tunnel-name]
credentials-file: /etc/cloudflared/[tunnel-id].json

ingress:
  - hostname: [your-subdomain].[your-domain]
    service: http://localhost:8080
  - service: http_status:404
```

**예시:**
```yaml
tunnel: mac-mini-umc
credentials-file: /etc/cloudflared/c8020eea-444c-41eb-85c8-302e025fe1cd.json

ingress:
  - hostname: spring-swagger-api.log8.kr
    service: http://localhost:8080
  - service: http_status:404
```

---

## 4. DNS 라우팅 설정

```bash
# 서브도메인을 터널에 연결
cloudflared tunnel route dns [tunnel-name] [your-subdomain].[your-domain]
```

**예시:**
```bash
cloudflared tunnel route dns mac-mini-umc spring-swagger-api.log8.kr
```

**출력:**
```
Added CNAME [your-subdomain].[your-domain] which will route to this tunnel.
```

---

## 5. 테스트

### 5.1 터널 실행 (테스트)

```bash
# Spring Boot 앱 실행 중인지 확인
curl http://localhost:8080/actuator/health

# 터널 실행 (테스트용)
cloudflared tunnel --config /etc/cloudflared/config.yml run [tunnel-name]
```

**성공 출력:**
```
INF Starting tunnel tunnelID=[tunnel-id]
INF Connection established
INF Registered tunnel connection
```

### 5.2 외부 접속 테스트

**새 터미널에서:**
```bash
# HTTPS 접속 테스트
curl https://[your-subdomain].[your-domain]/actuator/health
```

**성공 출력:**
```json
{"status":"UP"}
```

브라우저에서도 확인:
- `https://[your-subdomain].[your-domain]`
- `https://[your-subdomain].[your-domain]/swagger-ui.html`

---

## 6. 자동 시작 설정 (중요!)

### 6.1 plist 파일 생성

```bash
sudo nano /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
```

**전체 내용 입력:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.cloudflare.cloudflared</string>
    <key>ProgramArguments</key>
    <array>
        <string>/opt/homebrew/bin/cloudflared</string>
        <string>--config</string>
        <string>/etc/cloudflared/config.yml</string>
        <string>tunnel</string>
        <string>run</string>
        <string>[tunnel-name]</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/var/log/cloudflared.out.log</string>
    <key>StandardErrorPath</key>
    <string>/var/log/cloudflared.err.log</string>
</dict>
</plist>
```

**⚠️ 중요:** `[tunnel-name]`을 실제 터널 이름으로 변경!

### 6.2 권한 설정 및 서비스 시작

```bash
# 권한 설정
sudo chown root:wheel /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
sudo chmod 644 /Library/LaunchDaemons/com.cloudflare.cloudflared.plist

# 서비스 로드 및 시작
sudo launchctl load /Library/LaunchDaemons/com.cloudflare.cloudflared.plist

# 상태 확인
sudo launchctl list | grep cloudflare
```

**성공 출력:**
```
-    0    com.cloudflare.cloudflared
```
- 첫 번째 `-`: 프로세스 상태
- 두 번째 `0`: **Status (0 = 성공)**
- PID 숫자가 보이면 실행 중

**Status가 1이면 실패!** → 트러블슈팅 참고

### 6.3 로그 확인

```bash
# 실시간 로그 확인
sudo tail -f /var/log/cloudflared.out.log

# 에러 로그 확인
sudo tail -f /var/log/cloudflared.err.log
```

---

## 완료 체크리스트

### Cloudflare 설정
- [ ] cloudflared 설치
- [ ] Cloudflare 로그인
- [ ] 터널 생성
- [ ] config.yml 작성 (/etc/cloudflared/)
- [ ] DNS 라우팅 설정

### 테스트
- [ ] 터널 실행 테스트
- [ ] HTTPS 접속 성공
- [ ] Swagger UI 접속 확인

### 자동 시작
- [ ] plist 파일 생성 (ProgramArguments 완전히 작성)
- [ ] 서비스 로드 및 시작
- [ ] Status 0 확인
- [ ] 로그 확인

---

## 다음 단계

✅ Cloudflare Tunnel 설정 완료!

**다음 작업:**
- [GitHub Actions CI/CD 설정](CICD_SETUP.md) - 자동 배포

---

## 트러블슈팅

### Status가 1 (실패)

```bash
# plist 파일 확인
cat /Library/LaunchDaemons/com.cloudflare.cloudflared.plist

# ProgramArguments가 완전한지 확인:
# - /opt/homebrew/bin/cloudflared
# - --config
# - /etc/cloudflared/config.yml
# - tunnel
# - run
# - [tunnel-name]

# 서비스 재시작
sudo launchctl unload /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
sudo launchctl load /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
```

### 502 Bad Gateway

```bash
# Spring Boot 앱 확인
docker compose ps

# 로컬 접속 테스트
curl http://localhost:8080/actuator/health

# 컨테이너 재시작
docker compose restart
```

### DNS 전파 확인

```bash
# DNS 조회
nslookup [your-subdomain].[your-domain]

# CNAME 레코드 확인
dig [your-subdomain].[your-domain]
```

### Cloudflare 대시보드 확인

1. https://dash.cloudflare.com
2. **Zero Trust** → **Access** → **Tunnels**
3. 터널 상태 확인: **Healthy (초록색)**
