---
title: "맥미니 개봉부터 첫 배포까지 🖥️ - 홈서버의 첫걸음"
description: "맥미니 M2 언박싱, macOS 네트워크 설정, Docker 설치부터 Spring Boot 첫 실행까지의 여정"
pubDate: 2025-01-16
author: "SweetHeart"
tags: ["홈서버", "맥미니", "macOS", "Docker", "Spring Boot", "MySQL", "SSH"]
series: "맥미니 홈서버 구축기"
seriesOrder: 2
heroImage: "/blog/mac-mini-server/02-hero.jpg"
---

# 맥미니 개봉부터 첫 배포까지 🖥️

## 드디어 도착! 📦

택배 기사님이 작은 상자를 건네주셨습니다. 생각보다 정말 작네요!

```
┌─────────────────────────┐
│    Mac Mini M2 2023     │
│    16GB / 256GB SSD     │
│                         │
│    🍎 Apple             │
└─────────────────────────┘
크기: 19.7 × 19.7 × 3.6 cm
무게: 1.18 kg
```

상자를 열자마자 느낀 감상:
- **작다**: 손바닥만 함. 이게 서버라니!
- **가볍다**: 1kg 남짓. 들고 다닐 수 있을 정도
- **예쁘다**: 은색 알루미늄 바디. 역시 애플 디자인

## 첫 세팅: 모니터 연결 🖥️

홈서버로 쓸 예정이지만, 초기 설정은 모니터가 필요합니다.

### 필요한 것들

```
✅ 맥미니 본체
✅ 전원 케이블 (동봉)
✅ HDMI 모니터 (임시로 사용)
✅ USB 키보드
✅ USB 마우스 (또는 트랙패드)
✅ 인터넷 (Wi-Fi 또는 이더넷)
```

### 포트 확인

맥미니 M2 뒷면:

```
┌─────────────────────────────────────┐
│  🔌 전원                             │
│  🌐 이더넷 (Gigabit)                 │
│  🔊 헤드폰 잭                        │
│  🖥️ HDMI                            │
│  🔌 Thunderbolt 4 × 2               │
│  🔌 USB-A 3.0 × 2                   │
└─────────────────────────────────────┘
```

앞면에도 USB-C와 오디오 잭이 있어요!

## macOS 초기 설정 🍎

### 1. 전원 ON

전원 버튼은... 어디 있지? 🤔

**답: 뒷면 왼쪽 모서리**에 있습니다. 처음엔 찾기 어렵더라고요.

버튼을 누르면 **띵~** 하는 애플 특유의 시작음과 함께 부팅!

### 2. 환영 화면

```
Welcome to Mac

언어 선택
- 한국어

키보드 레이아웃
- 한국어

Wi-Fi 연결
- 집 공유기 연결
```

### 3. 계정 생성

여기가 중요합니다!

```
사용자 이름: sweetheart
계정 이름: sweetheart
비밀번호: ********** (복잡하게!)
힌트: (잊어버릴 것 대비)
```

⚠️ **주의사항:**
- 계정 이름은 나중에 SSH 접속 시 사용됩니다
- 간단하고 기억하기 쉬운 이름으로!
- 비밀번호는 나중에 sudo 권한 때 필요해요

### 4. Apple ID (스킵!)

Apple ID 로그인은 **건너뛰기**했습니다.
- 서버용이라 굳이 필요 없음
- iCloud 동기화도 필요 없음

### 5. 설정 완료! ✨

드디어 macOS Sonoma 데스크톱이 보입니다!

## 네트워크 설정: 고정 IP 필수! 🌐

### 왜 고정 IP가 필요한가?

홈서버는 **항상 같은 IP**를 가져야 합니다.
- SSH 접속 시 주소 변경 방지
- Cloudflare Tunnel 설정 안정성
- 방화벽 규칙 설정 편의성

### 현재 IP 확인

터미널을 엽니다 (Cmd + Space → "터미널" 검색)

```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

출력:
```
inet 192.168.0.143 netmask 0xffffff00 broadcast 192.168.0.255
```

현재 IP: `192.168.0.143` (DHCP로 자동 할당됨)

### 고정 IP 설정

1. **시스템 설정** → **네트워크** 클릭
2. **Wi-Fi** (또는 이더넷) 선택
3. **세부사항** 클릭
4. **TCP/IP** 탭 선택
5. **IPv4 구성**: `수동으로` 변경

설정 값:
```
IPv4 주소: 192.168.0.61
서브넷 마스크: 255.255.255.0
라우터: 192.168.0.1
```

6. **DNS** 탭 이동
```
DNS 서버:
  8.8.8.8
  8.8.4.4
```

7. **확인** 클릭

### IP 변경 확인

```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

출력:
```
inet 192.168.0.61 netmask 0xffffff00 broadcast 192.168.0.255
```

완벽! 이제 `192.168.0.61`로 고정되었습니다. ✅

## SSH 원격 접속 설정 🔐

이제 모니터를 떼고 Windows PC에서 원격으로 관리하려고 합니다!

### SSH 활성화

1. **시스템 설정** → **공유**
2. **원격 로그인** 토글 ON ✅
3. **접근 허용**: `모든 사용자` 선택

### Windows에서 SSH 접속 테스트

PowerShell 또는 CMD를 엽니다.

```bash
ssh sweetheart@192.168.0.61
```

첫 접속 시 fingerprint 확인:
```
The authenticity of host '192.168.0.61' can't be established.
ED25519 key fingerprint is SHA256:...
Are you sure you want to continue connecting (yes/no)? yes
```

비밀번호 입력:
```
sweetheart@192.168.0.61's password:
```

성공! 🎉
```
Last login: ...
sweetheart@Mac-Mini ~ %
```

이제 **모니터를 뗄 수 있습니다!** 🎊

모니터, 키보드, 마우스를 모두 분리하고 맥미니를 책상 구석에 놓았습니다. 이제 진짜 서버처럼 동작하네요!

## Homebrew 설치 🍺

macOS의 필수 패키지 매니저!

### 설치 명령어

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

설치 진행... (약 5분 소요)

### PATH 추가 (M1/M2 Mac)

Apple Silicon Mac은 Homebrew 경로가 다릅니다:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
source ~/.zprofile
```

### 설치 확인

```bash
brew --version
```

출력:
```
Homebrew 4.2.0
```

완료! ✅

## Java 21 설치 ☕

Spring Boot 3.x는 Java 17 이상이 필요합니다. Java 21 LTS를 설치하죠!

### 설치

```bash
brew install openjdk@21
```

### 심볼릭 링크 생성

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

### 확인

```bash
java -version
```

출력:
```
openjdk version "21.0.1" 2023-10-17
OpenJDK Runtime Environment Homebrew (build 21.0.1)
OpenJDK 64-Bit Server VM Homebrew (build 21.0.1, mixed mode, sharing)
```

완벽! ☕✅

## Docker Desktop 설치 🐳

컨테이너로 Spring Boot와 MySQL을 관리할 거예요!

### 설치

```bash
brew install --cask docker
```

설치 후 확인:
```
==> Downloading https://desktop.docker.com/mac/main/arm64/Docker.dmg
==> Installing Cask docker
```

### Docker Desktop 실행

**Finder** → **Applications** → **Docker** 더블클릭

처음 실행 시:
1. 약관 동의
2. (선택) Docker Hub 로그인 (스킵 가능)
3. **Start Docker Desktop when you log in** ✅ 체크 (중요!)

### 확인

터미널에서:
```bash
docker --version
docker compose version
```

출력:
```
Docker version 24.0.7, build afdd53b
Docker Compose version v2.23.3
```

완벽! 🐳✅

## 프로젝트 클론 📂

### Git 설정

```bash
git config --global user.name "SweetHeart"
git config --global user.email "your-email@example.com"
```

### 프로젝트 클론

```bash
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/UMC-CAU/umc-9th-springboot-sweetheart.git
cd umc-9th-springboot-sweetheart
```

### 디렉토리 구조 확인

```bash
ls -la
```

```
.
├── .github/
├── docs/
├── src/
├── build.gradle
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── README.md
```

좋아요! ✅

## 환경 변수 설정 🔐

### .env 파일 생성

```bash
cp .env.example .env
nano .env
```

### 환경 변수 입력

```env
# MySQL Root Password
DB_ROOT_PASSWORD=your_strong_root_password_here

# MySQL User
DB_USER=umc_user
DB_PW=your_strong_user_password_here

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=prod
DDL_AUTO=update
SHOW_SQL=true
```

⚠️ **보안 주의:**
- 비밀번호는 반드시 강력하게!
- `.env` 파일은 절대 Git에 커밋하지 말 것
- `.gitignore`에 포함되어 있는지 확인

저장: `Ctrl + O` → `Enter` → `Ctrl + X`

## Docker Compose로 첫 실행! 🚀

드디어! Spring Boot + MySQL을 실행할 시간입니다.

### docker-compose.yml 확인

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: umc-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: umc9th
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PW}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - umc-network

  backend:
    build: .
    container_name: umc-backend
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/umc9th
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PW}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      SPRING_JPA_HIBERNATE_DDL_AUTO: ${DDL_AUTO}
      SPRING_JPA_SHOW_SQL: ${SHOW_SQL}
    ports:
      - "8080:8080"
    networks:
      - umc-network

volumes:
  mysql_data:

networks:
  umc-network:
    driver: bridge
```

### 빌드 & 실행

```bash
docker compose up --build -d
```

출력:
```
[+] Building 45.2s (12/12) FINISHED
 => [backend internal] load build definition
 => => transferring dockerfile: 324B
 => [backend] building image...

[+] Running 3/3
 ✔ Network umc-network       Created
 ✔ Container umc-mysql        Started
 ✔ Container umc-backend      Started
```

빌드 시간: 약 2~3분 (처음엔 길어요)

### 상태 확인

```bash
docker compose ps
```

출력:
```
NAME            IMAGE               STATUS          PORTS
umc-mysql       mysql:8.0           Up 30 seconds   0.0.0.0:3306->3306/tcp
umc-backend     umc-backend:latest  Up 15 seconds   0.0.0.0:8080->8080/tcp
```

모두 **Up** 상태! ✅

### 로그 확인

```bash
docker compose logs -f backend
```

Spring Boot 시작 로그:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.5.6)

...
Started Umc9thApplication in 3.456 seconds
```

성공! 🎉

## 첫 Health Check ✅

### localhost 테스트

```bash
curl http://localhost:8080/actuator/health
```

응답:
```json
{
  "status": "UP"
}
```

**완벽합니다!** 🎊

### Swagger UI 확인

맥미니에서 Safari 열고:
```
http://localhost:8080/swagger-ui.html
```

Swagger UI가 보입니다! API 문서가 예쁘게 렌더링되네요. 📝

### Windows PC에서 접속 테스트

PowerShell에서:
```bash
curl http://192.168.0.61:8080/actuator/health
```

응답:
```json
{
  "status": "UP"
}
```

**로컬 네트워크에서도 접속됩니다!** ✅

## MySQL 접속 확인 🗄️

### MySQL CLI 접속

```bash
docker compose exec mysql mysql -u umc_user -p umc9th
```

비밀번호 입력 후:
```
mysql> SHOW TABLES;
```

출력:
```
+------------------+
| Tables_in_umc9th |
+------------------+
| member           |
| food             |
| member_food      |
| term             |
+------------------+
```

JPA가 자동으로 테이블을 생성했네요! Hibernate의 `ddl-auto: update`가 작동한 거죠. ✨

### 데이터 확인

```sql
SELECT * FROM member;
```

```
Empty set (0.00 sec)
```

아직 데이터는 없지만, 테이블은 정상적으로 생성되었습니다!

```sql
exit
```

## Chrome Remote Desktop 설정 (선택) 🖥️

SSH로 모든 걸 할 수 있지만, 가끔 GUI가 필요할 때도 있어요.

### 설치

1. 맥미니에서 Chrome 설치
```bash
brew install --cask google-chrome
```

2. https://remotedesktop.google.com/access 접속
3. **이 기기 설정** 클릭
4. 지시에 따라 설치

### Windows에서 접속

1. Windows PC에서 https://remotedesktop.google.com/access
2. 맥미니 선택
3. PIN 입력

**원격으로 macOS 화면이 보입니다!** 🎉

이제 정말 모니터 없이도 완전히 제어 가능하네요!

## 자동 시작 설정 🔄

맥미니가 재부팅되어도 자동으로 시작되게 설정합니다.

### 1. Docker Desktop 자동 시작

**Docker Desktop** → **Settings** → **General**
- ✅ **Start Docker Desktop when you log in**

### 2. macOS 자동 로그인

**시스템 설정** → **사용자 및 그룹** → **자동 로그인**
- 사용자 선택: `sweetheart`
- 비밀번호 입력

⚠️ **보안 고려사항:**
- 물리적 보안이 확보된 장소에서만 권장
- 집 안에서만 사용하면 OK

### 3. Docker Compose 자동 시작

LaunchAgent를 만들어볼까 했는데... Docker Desktop이 시작되면 컨테이너도 자동으로 시작되도록 설정할 수 있어요!

```bash
cd ~/projects/umc-9th-springboot-sweetheart
docker compose up -d
docker update --restart unless-stopped umc-mysql
docker update --restart unless-stopped umc-backend
```

이제 맥미니를 재부팅해도 자동으로 서비스가 시작됩니다! ✨

## 리소스 사용량 확인 📊

### Docker 리소스

```bash
docker stats --no-stream
```

출력:
```
CONTAINER      CPU %    MEM USAGE / LIMIT     MEM %
umc-backend    0.15%    512MiB / 16GiB        3.13%
umc-mysql      0.08%    256MiB / 16GiB        1.56%
```

메모리를 **768MB**만 사용하고 있네요! 16GB 중 5%도 안 씀. 여유롭습니다! 😎

### 시스템 전체

```bash
top -l 1 | head -n 10
```

```
Processes: 327 total
Load Avg: 1.23, 1.45, 1.34
CPU usage: 2.5% user, 1.2% sys, 96.3% idle
PhysMem: 3.2G used, 12.8G unused
```

**CPU 사용률: 3.7%**
**메모리 사용: 3.2GB / 16GB (20%)**

완전 여유롭습니다! M2 칩의 효율이 대단하네요. 🍎

## 전기세 측정 ⚡

와트 미터를 연결해서 실측했습니다.

```
🔌 전력 소비량:
- 유휴 상태: 7W
- 일반 사용 (Docker 실행): 12W
- 최대 부하: 18W
```

**월 전기세 계산:**
```
12W × 24시간 × 30일 = 8.64 kWh
8.64 kWh × ₩150/kWh = ₩1,296

약 ₩1,300/월
```

AWS RDS(₩24,000/월)의 **5%** 수준! 😱

## 첫 배포 완료! 🎉

축하합니다! 우리는 방금:

✅ 맥미니 M2 개봉 & 초기 설정
✅ 고정 IP 설정 (192.168.0.61)
✅ SSH 원격 접속 활성화
✅ Homebrew, Java 21, Docker 설치
✅ Spring Boot + MySQL Docker Compose 실행
✅ localhost:8080 첫 실행 성공!
✅ 로컬 네트워크 접속 확인
✅ 자동 시작 설정
✅ 전기세 측정 (₩1,300/월)

## 현재 상태 점검

### 접속 가능 URL

```
✅ 맥미니 로컬: http://localhost:8080
✅ 같은 네트워크: http://192.168.0.61:8080
❌ 외부 인터넷: 아직 안 됨 (다음 편에서!)
```

### 아직 남은 과제

1. **외부 접속**: 인터넷에서 접속 불가
2. **HTTPS**: HTTP만 가능 (보안 취약)
3. **자동 배포**: 수동으로 git pull 해야 함
4. **모니터링**: 로그 확인 불편

이 모든 걸 다음 편들에서 해결합니다!

## 다음 편 예고

이제 **외부에서 HTTPS로 접속**하게 만들어야죠!

포트 포워딩? ❌
공인 IP? ❌
공유기 설정? ❌

**Cloudflare Tunnel**로 모든 걸 해결합니다! 🚀

다음 편에서는:
- 🔒 **HTTPS 자동 인증서**
- 🌐 **외부에서 접속 가능**
- 🛡️ **DDoS 보호**
- 💰 **완전 무료!**

> **3편: 포트 포워딩 없이 HTTPS 열기 - Cloudflare Tunnel 🔒** (Coming Soon)

---

## 마치며

맥미니를 서버로 만드는 첫 단계를 완료했습니다!

모니터를 떼고 책상 구석에 조용히 돌아가는 맥미니를 보니 뿌듯하네요. 이게 바로 **내 서버**라니! 😊

질문이나 궁금한 점 있으면 댓글 남겨주세요!

다음 편에서 만나요! 👋

---

## 시리즈 목차

1. AWS 요금 폭탄 💸에서 맥미니 홈서버로 탈출하기
2. **맥미니 개봉부터 첫 배포까지 🖥️** ← 현재
3. 포트 포워딩 없이 HTTPS 열기 - Cloudflare Tunnel 🔒
4. git push만으로 자동 배포 - Self-Hosted Runner 🚀
5. 삽질 기록 - 트러블슈팅 모음집 🔧
6. 맥미니 홈서버 1개월 후기 & 최종 정산 💰

---

**Tags:** #홈서버 #맥미니 #macOS #Docker #SpringBoot #MySQL #SSH #M2
