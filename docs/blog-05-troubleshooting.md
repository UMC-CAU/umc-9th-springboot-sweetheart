---
title: "맥미니 홈서버 삽질 기록 🔧 - 새벽 2시의 디버깅 전쟁"
description: "Cloudflare Status 1부터 Shift 키 오작동까지, 홈서버 구축하며 겪은 모든 에러와 해결 방법 총정리"
pubDate: 2025-01-19
author: "SweetHeart"
tags: ["트러블슈팅", "디버깅", "에러해결", "홈서버", "DevOps"]
series: "맥미니 홈서버 구축기"
seriesOrder: 5
heroImage: "/blog/mac-mini-server/05-hero.jpg"
---

# 맥미니 홈서버 삽질 기록 🔧

## 프롤로그: 에러는 배움의 시작

홈서버 구축 과정은 순탄치 않았습니다.

```
✅ 성공: 10%
🔧 삽질: 90%
```

하지만 **이 90%의 삽질**이 제게 가장 큰 배움을 주었죠. 😅

이번 편에서는 제가 겪은 모든 에러와 해결 방법을 공유합니다. 여러분은 제 삽질을 반복하지 마세요!

---

## 🔥 에러 #1: Cloudflare Tunnel Status 1 지옥

### 상황

```bash
sudo launchctl list | grep cloudflare
```

출력:
```
-    1    com.cloudflare.cloudflared
```

**Status: 1** (실패!) ❌

### 증상

- Cloudflare Tunnel이 시작되지 않음
- 로그 파일에 아무것도 안 씀
- 수동 실행은 됨

### 첫 번째 시도: 로그 확인

```bash
sudo tail /var/log/cloudflared.err.log
```

출력:
```
(비어있음)
```

**...뭐라고?** 😱

에러가 나는데 로그가 없다니!

### 두 번째 시도: 수동 실행

혹시 명령어가 틀렸나?

```bash
/opt/homebrew/bin/cloudflared --config /etc/cloudflared/config.yml tunnel run mac-mini-umc
```

출력:
```
INF Starting tunnel tunnelID=c8020eea...
INF Connection registered
```

**이건 잘 됨!** 😵

그럼 plist 파일이 문제?

### 세 번째 시도: plist 파일 확인

```bash
cat /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
```

plist 내용:
```xml
<key>ProgramArguments</key>
<array>
    <string>/opt/homebrew/bin/cloudflared</string>
</array>
```

**!!!!!!**

**문제 발견!** 🔍

`tunnel run mac-mini-umc` 부분이 완전히 빠져있었어요!

### 해결 방법

plist 파일 수정:

```xml
<key>ProgramArguments</key>
<array>
    <string>/opt/homebrew/bin/cloudflared</string>
    <string>--config</string>
    <string>/etc/cloudflared/config.yml</string>
    <string>tunnel</string>
    <string>run</string>
    <string>mac-mini-umc</string>
</array>
```

**각 인자를 별도 `<string>` 태그로!**

서비스 재시작:

```bash
sudo launchctl unload /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
sudo launchctl load /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
```

상태 확인:

```bash
sudo launchctl list | grep cloudflare
```

출력:
```
73421    0    com.cloudflare.cloudflared
```

**PID 출현! Status 0!** 🎉

### 교훈

- macOS LaunchDaemon의 `ProgramArguments`는 **각 인자를 별도 태그**로!
- 공백으로 구분된 문자열이 아님
- 수동 실행과 서비스 실행은 다를 수 있음

### 소요 시간

**3시간** (새벽 1시 ~ 4시) 😭

---

## 💀 에러 #2: SSH 비밀번호 입력 오류

### 상황

맥미니 SSH 접속 시도:

```bash
ssh sweetheart@192.168.0.61
```

비밀번호 입력:
```
sweetheart@192.168.0.61's password: ********
Permission denied, please try again.
```

**비밀번호가 틀렸다고?** 😨

분명 맞는데...

### 첫 번째 시도: 비밀번호 확인

```
내 비밀번호: Test$1234
```

몇 번을 입력해도 실패!

### 두 번째 시도: 비밀번호 초기화?

Chrome Remote Desktop으로 맥미니 접속 → 비밀번호 변경하려는데...

### 문제 발견!

Chrome Remote Desktop에서 직접 입력하니까 **로그인 성공!** 🤔

Windows 키보드와 macOS 키보드 차이?

### 원인 분석

**원격 데스크톱에서 우측 Shift 키가 먹통!** 😱

```
Test$1234  ← $ 입력 안 됨 (우측 Shift 사용)
Test41234  ← 실제 입력된 값
```

우측 Shift + 4 = $ (물리 키보드)
하지만 원격에서는 Shift가 안 먹혀서 그냥 4!

### 해결 방법

**좌측 Shift 사용** 또는 **비밀번호를 특수문자 없는 걸로 변경**

```bash
# macOS에서 비밀번호 변경
passwd
```

새 비밀번호: `TestPassword123` (특수문자 없이)

### SSH 재시도

```bash
ssh sweetheart@192.168.0.61
```

비밀번호 입력: `TestPassword123`

```
Last login: ...
sweetheart@Mac-Mini ~ %
```

**성공!** 🎉

### 교훈

- 원격 데스크톱 키보드 이슈 조심
- 비밀번호는 단순할수록 좋음 (특수문자 주의)
- SSH 키 인증 사용하면 이런 문제 없음!

### 소요 시간

**1시간** (짜증 포함) 😤

---

## 🐳 에러 #3: Docker Compose 빌드 무한 로딩

### 상황

```bash
docker compose up --build -d
```

출력:
```
[+] Building 856.2s (5/12)
 => [backend internal] load build definition
 => => transferring dockerfile: 324B
 => [backend] building...
```

**10분째 멈춰있음!** ⏳

### 증상

- Docker 빌드가 특정 단계에서 멈춤
- CPU 사용률 0%
- 로그 없음

### 첫 번째 시도: 재시작

```bash
Ctrl + C
docker compose up --build -d
```

똑같음!

### 두 번째 시도: 로그 확인

```bash
docker compose logs backend
```

출력:
```
(비어있음)
```

### 세 번째 시도: 빌드 캐시 삭제

```bash
docker builder prune -a
```

출력:
```
Total reclaimed space: 2.4GB
```

다시 빌드:

```bash
docker compose up --build -d
```

**여전히 느림!** 😫

### 문제 발견: Docker Desktop 메모리 부족

**활동 모니터** 확인:
```
Docker.app: 15.8GB / 16GB (99%)
```

**메모리 가득 참!** 😱

### 해결 방법

Docker Desktop 재시작:

**Applications** → **Docker** → **Restart**

메모리 해제 확인:
```
Docker.app: 512MB / 16GB
```

다시 빌드:

```bash
docker compose up --build -d
```

출력:
```
[+] Building 45.2s (12/12) FINISHED
 ✔ backend
```

**2분 만에 완료!** 🎉

### 교훈

- Docker Desktop은 메모리를 많이 먹음
- 주기적으로 재시작 필요
- `docker system prune` 정기 실행

### 소요 시간

**30분** (답답함 포함) 😤

---

## 🗄️ 에러 #4: MySQL 컨테이너 연결 실패

### 상황

Spring Boot 로그:

```
docker compose logs backend
```

출력:
```
Caused by: java.net.ConnectException: Connection refused
  at com.mysql.cj.protocol.StandardSocketFactory.connect
```

**MySQL 연결 거부!** ❌

### 첫 번째 시도: MySQL 상태 확인

```bash
docker compose ps
```

출력:
```
NAME         STATUS
umc-mysql    Up 30 seconds (healthy)
umc-backend  Restarting (1) 5 seconds ago
```

MySQL은 정상인데?

### 두 번째 시도: 환경 변수 확인

```bash
cat .env
```

출력:
```
DB_ROOT_PASSWORD=rootpassword123
DB_USER=umc_user
DB_PW=userpassword123
```

application.yml:
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/umc9th
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
```

docker-compose.yml:
```yaml
backend:
  environment:
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/umc9th
    SPRING_DATASOURCE_USERNAME: ${DB_USER}
    SPRING_DATASOURCE_PASSWORD: ${DB_PW}
```

**변수 이름이 달라!** 😵

### 문제 발견

- `.env`: `DB_USER`, `DB_PW`
- `docker-compose.yml`: `SPRING_DATASOURCE_USERNAME`으로 전달
- `application.yml`: `${SPRING_DATASOURCE_USERNAME}` 기대

**일치함!** 그럼 뭐가 문제?

### 세 번째 시도: 타이밍 문제?

MySQL이 완전히 시작되기 전에 Spring Boot가 연결 시도?

docker-compose.yml 수정:

```yaml
backend:
  depends_on:
    mysql:
      condition: service_healthy  # ✨ 추가!
```

mysql에 healthcheck 추가:

```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 5s
    timeout: 3s
    retries: 10
```

### 재시작

```bash
docker compose down
docker compose up -d
```

출력:
```
[+] Running 2/2
 ✔ Container umc-mysql    Healthy
 ✔ Container umc-backend  Started
```

**MySQL Healthy 확인 후 backend 시작!** ✅

Spring Boot 로그:
```
HikariPool-1 - Start completed.
Started Umc9thApplication in 3.456 seconds
```

**성공!** 🎉

### 교훈

- `depends_on`만으로는 부족
- `condition: service_healthy` 사용
- healthcheck 설정 필수

### 소요 시간

**45분** 😓

---

## 🚫 에러 #5: GitHub Actions SSH 타임아웃

### 상황

GitHub Actions 로그:

```
Run appleboy/ssh-action@master
======CMD======
cd ~/projects/umc-9th-springboot-sweetheart
...
======END======
err: dial tcp 192.168.0.61:22: i/o timeout
Error: Process completed with exit code 255.
```

**타임아웃!** ❌

### 원인

- GitHub Actions는 클라우드에서 실행
- 맥미니는 로컬 네트워크 (192.168.0.61)
- 클라우드 → 로컬 접속 불가능!

### 실패한 해결 시도들

#### 1. 공인 IP + 포트 포워딩?

```
❌ 보안 위험
❌ 유동 IP
❌ 공유기 의존
```

#### 2. Cloudflare Tunnel for SSH?

```
❌ 복잡함
❌ 추가 설정
```

### 해결 방법: Self-Hosted Runner

**GitHub Actions를 맥미니에서 실행!**

```bash
cd ~/actions-runner
./config.sh --url https://github.com/[org]/[repo] --token [TOKEN]
./svc.sh install
./svc.sh start
```

deploy.yml 수정:

```yaml
jobs:
  deploy:
    runs-on: self-hosted  # ✨ 변경!
```

**SSH 불필요!** 로컬에서 직접 실행!

### 교훈

- 로컬 네트워크 서버는 Self-Hosted Runner 필수
- 클라우드 CI/CD의 한계 인식
- 홈서버의 제약 조건 이해

### 소요 시간

**2시간** (삽질 + 학습) 📚

---

## ⏱️ 에러 #6: Docker Compose Health Check 실패

### 상황

배포 스크립트:

```bash
docker compose up --build -d
curl -f http://localhost:8080/actuator/health
```

출력:
```
curl: (7) Failed to connect to localhost port 8080
```

**Health check 실패!** ❌

### 원인

Spring Boot가 완전히 시작되기 전에 curl 실행!

### 해결 방법 1: sleep 추가

```bash
docker compose up --build -d
sleep 10  # ✨ 10초 대기
curl -f http://localhost:8080/actuator/health
```

**동작함!** 하지만 항상 10초 기다려야 함...

### 해결 방법 2: Retry 로직

```bash
docker compose up --build -d

# Health check with retry
for i in {1..30}; do
  if curl -f http://localhost:8080/actuator/health; then
    echo "✅ Health check passed!"
    break
  fi
  echo "⏳ Waiting... ($i/30)"
  sleep 2
done
```

**최대 1분 대기, 성공 시 즉시 종료!** ✅

### 교훈

- 컨테이너 시작 ≠ 앱 준비 완료
- Health check는 필수
- Retry 로직으로 탄력성 확보

---

## 🎯 삽질 통계 요약

### 에러별 소요 시간

| 에러 | 소요 시간 | 난이도 |
|------|----------|--------|
| Cloudflare Status 1 | 3시간 | ⭐⭐⭐⭐⭐ |
| SSH 비밀번호 | 1시간 | ⭐⭐ |
| Docker 빌드 무한 로딩 | 30분 | ⭐⭐⭐ |
| MySQL 연결 실패 | 45분 | ⭐⭐⭐⭐ |
| GitHub Actions 타임아웃 | 2시간 | ⭐⭐⭐⭐ |
| Health Check 실패 | 30분 | ⭐⭐ |
| **총합** | **8시간** | 😭 |

### 삽질 시간대

```
🌙 새벽 1~4시: 50% (제일 많이 삽질함)
☀️ 오후 2~6시: 30%
🌆 저녁 8~11시: 20%
```

**새벽이 제일 위험합니다!** 😂

### 가장 도움된 디버깅 방법

1. **로그 확인** (50%)
2. **수동 실행 테스트** (30%)
3. **Google 검색** (15%)
4. **재시작** (5%)

---

## 🛠️ 삽질을 줄이는 팁

### 1. 로그부터 확인

```bash
# Docker 로그
docker compose logs -f

# 시스템 로그 (macOS)
sudo tail -f /var/log/system.log

# 서비스 로그
sudo tail -f /var/log/cloudflared.out.log
```

### 2. 수동 실행으로 테스트

서비스가 안 되면 **수동으로 먼저 실행**:

```bash
# 수동 실행
/opt/homebrew/bin/cloudflared --config ... tunnel run ...

# 잘 되면 → 서비스 설정 문제
# 안 되면 → 명령어 자체 문제
```

### 3. 환경 변수 출력

```bash
# .env 파일 확인
cat .env

# Docker 컨테이너 환경 변수 확인
docker compose exec backend env | grep SPRING
```

### 4. 단계별 검증

한 번에 다 하지 말고 단계별로:

1. ✅ localhost 접속
2. ✅ 로컬 네트워크 접속
3. ✅ Cloudflare Tunnel 설정
4. ✅ 자동 배포 설정

### 5. 변경사항 Git 커밋

작동하는 상태를 항상 커밋:

```bash
git add .
git commit -m "feat: Working state before trying X"
```

잘못되면 되돌리기 쉬움!

---

## 다음 편 예고

이제 모든 삽질을 끝내고 **1개월간 안정적으로 운영**했습니다!

다음 편에서는:
- 📊 **실제 비용 측정** (전기세, 유지보수)
- ⚡ **성능 측정** (응답 시간, 부하 테스트)
- 💰 **AWS vs 맥미니 최종 비교**
- 📈 **트래픽 통계**
- ✅ **장단점 솔직 후기**
- 🔮 **다음 계획**

> **6편: 맥미니 홈서버 1개월 후기 & 최종 정산 💰** (Coming Soon)

---

## 마치며

8시간의 삽질, 그리고 무수한 에러...

하지만 이 모든 과정이 저를 성장시켰습니다. 💪

**에러는 두렵지 않아요. 배움의 기회니까요!** 😊

여러분도 홈서버 구축 중 막히면, 이 글을 참고하세요!
댓글로 여러분의 삽질 경험도 공유해주세요! 💬

---

## 시리즈 목차

1. AWS 요금 폭탄 💸에서 맥미니 홈서버로 탈출하기
2. 맥미니 개봉부터 첫 배포까지 🖥️
3. 포트 포워딩 없이 HTTPS 열기 - Cloudflare Tunnel 🔒
4. git push만으로 자동 배포 - Self-Hosted Runner 🚀
5. **삽질 기록 - 트러블슈팅 모음집 🔧** ← 현재
6. 맥미니 홈서버 1개월 후기 & 최종 정산 💰

---

**Tags:** #트러블슈팅 #디버깅 #에러해결 #홈서버 #DevOps #삽질 #학습
