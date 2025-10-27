# 맥미니 초기 설정 가이드

## 목차
1. [네트워크 확인 및 고정 IP 설정](#1-네트워크-확인-및-고정-ip-설정)
2. [필수 소프트웨어 설치](#2-필수-소프트웨어-설치)
3. [Docker 및 Docker Compose 설치](#3-docker-및-docker-compose-설치)
4. [MySQL 데이터베이스 설정](#4-mysql-데이터베이스-설정)
5. [SSH 접속 설정](#5-ssh-접속-설정)
6. [방화벽 설정](#6-방화벽-설정)
7. [프로젝트 클론 및 테스트](#7-프로젝트-클론-및-테스트)

---

## 1. 네트워크 확인 및 고정 IP 설정

### 1.1 현재 IP 주소 확인

맥미니 터미널에서:

```bash
# 현재 IP 주소 확인
ifconfig | grep "inet " | grep -v 127.0.0.1

# 또는 더 간단하게
ipconfig getifaddr en0  # Wi-Fi
ipconfig getifaddr en1  # 유선 (있는 경우)
```

출력 예시:
```
192.168.0.123
```

### 1.2 고정 IP 설정 (중요!)

**왜 필요한가?**
- GitHub Actions가 SSH로 접속할 때 IP가 변하면 안 됨
- Cloudflare 터널 설정 시 안정적인 연결 필요

**설정 방법:**

1. **시스템 설정** 열기
2. **네트워크** 클릭
3. 사용 중인 네트워크 선택 (Wi-Fi 또는 이더넷)
4. **세부사항...** 클릭
5. **TCP/IP** 탭 선택
6. **IPv4 구성**: `DHCP 사용` → `수동으로` 변경
7. 다음 정보 입력:
   ```
   IPv4 주소: 192.168.0.123 (현재 IP 사용)
   서브넷 마스크: 255.255.255.0
   라우터: 192.168.0.1 (공유기 IP, 보통 게이트웨이와 동일)
   ```
8. **DNS 서버** 탭:
   ```
   8.8.8.8
   8.8.4.4
   ```
9. **확인** 클릭

### 1.3 고정 IP 확인

```bash
# IP가 제대로 고정되었는지 확인
ipconfig getifaddr en0

# 인터넷 연결 확인
ping -c 3 google.com
```

---

## 2. 필수 소프트웨어 설치

### 2.1 Homebrew 설치 (macOS 패키지 관리자)

```bash
# Homebrew 설치
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 설치 확인
brew --version
```

### 2.2 Git 설치

```bash
# Git 설치 (보통 이미 설치되어 있음)
brew install git

# Git 버전 확인
git --version

# Git 설정
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

### 2.3 Java 21 설치

```bash
# OpenJDK 21 설치
brew install openjdk@21

# 환경 변수 설정
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Java 버전 확인
java -version
# 출력: openjdk version "21.0.x"
```

---

## 3. Docker 및 Docker Compose 설치

### 3.1 Docker Desktop 설치 (추천)

**방법 1: 웹사이트에서 다운로드**
1. https://www.docker.com/products/docker-desktop 접속
2. Mac (Apple Silicon 또는 Intel) 선택
3. Docker Desktop 설치 파일 다운로드
4. 설치 후 실행

**방법 2: Homebrew로 설치**

```bash
# Docker Desktop 설치
brew install --cask docker

# Docker Desktop 실행 (Spotlight에서 "Docker" 검색 후 실행)
```

### 3.2 Docker 설치 확인

```bash
# Docker 버전 확인
docker --version
# 출력: Docker version 24.0.x

# Docker Compose 버전 확인
docker compose version
# 출력: Docker Compose version v2.x.x

# Docker 실행 확인
docker ps
# 출력: CONTAINER ID   IMAGE   COMMAND   CREATED   STATUS   PORTS   NAMES
```

### 3.3 Docker 권한 설정

```bash
# 현재 사용자를 docker 그룹에 추가 (필요시)
# macOS에서는 보통 자동으로 설정됨

# Docker 없이 sudo 없이 실행 가능한지 확인
docker run hello-world
```

---

## 4. MySQL 데이터베이스 설정

### 4.1 MySQL 설치 (Docker로 설치할 예정이므로 선택 사항)

**옵션 A: Docker Compose로 MySQL 실행 (추천)**
- 프로젝트의 `docker-compose.yml`에 이미 MySQL 포함
- 별도 설치 불필요

**옵션 B: 로컬에 MySQL 직접 설치**

```bash
# MySQL 설치
brew install mysql

# MySQL 서비스 시작
brew services start mysql

# MySQL 보안 설정
mysql_secure_installation

# MySQL 접속 확인
mysql -u root -p
```

### 4.2 데이터베이스 생성 (Docker 사용 시 자동)

Docker Compose를 사용하면 자동으로 생성되지만, 수동으로 하려면:

```sql
-- MySQL 접속 후
CREATE DATABASE umc9th CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성
CREATE USER 'umc_user'@'%' IDENTIFIED BY 'your_secure_password';

-- 권한 부여
GRANT ALL PRIVILEGES ON umc9th.* TO 'umc_user'@'%';
FLUSH PRIVILEGES;

-- 확인
SHOW DATABASES;
```

---

## 5. SSH 접속 설정

### 5.1 SSH 서버 활성화

1. **시스템 설정** 열기
2. **일반** → **공유** 클릭
3. **원격 로그인** 활성화
4. 접근 권한: **관리자만** 또는 **모든 사용자**

### 5.2 SSH 접속 테스트 (Windows 데스크톱에서)

```bash
# Windows 터미널에서 맥미니로 SSH 접속 테스트
ssh your-username@192.168.0.123

# 비밀번호 입력 후 접속 성공하면 OK
```

### 5.3 SSH 키 생성 및 등록 (비밀번호 없이 접속)

**Windows 데스크톱에서:**

```bash
# SSH 키 생성 (이미 있다면 스킵)
ssh-keygen -t ed25519 -C "your-email@example.com"
# 기본 경로: C:\Users\YourName\.ssh\id_ed25519

# 공개 키 복사
cat ~/.ssh/id_ed25519.pub
```

**맥미니에서:**

```bash
# authorized_keys 파일에 공개 키 추가
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
# (공개 키 붙여넣기)

# 파일 권한 설정
chmod 600 ~/.ssh/authorized_keys
```

**Windows에서 비밀번호 없이 접속 테스트:**

```bash
ssh your-username@192.168.0.123
# 비밀번호 없이 접속되면 성공!
```

### 5.4 SSH 설정 최적화 (맥미니)

```bash
# SSH 설정 파일 수정
sudo nano /etc/ssh/sshd_config

# 다음 내용 확인/수정
PubkeyAuthentication yes
PasswordAuthentication yes  # 필요시 no로 변경 (키만 사용)
PermitRootLogin no
```

```bash
# SSH 서비스 재시작
sudo launchctl stop com.openssh.sshd
sudo launchctl start com.openssh.sshd
```

---

## 6. 방화벽 설정

### 6.1 macOS 방화벽 설정

1. **시스템 설정** 열기
2. **네트워크** → **방화벽** 클릭
3. **방화벽 옵션...**
4. 다음 포트 허용:
   - SSH (22)
   - HTTP (8080) - Spring Boot
   - MySQL (3306) - 필요시

**또는 터미널에서:**

```bash
# 방화벽 상태 확인
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# 특정 앱 허용 (Docker)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /Applications/Docker.app
```

### 6.2 공유기 포트 포워딩 (외부 접속이 필요한 경우)

**주의:** Cloudflare 터널을 사용하면 포트 포워딩 불필요!

만약 직접 외부 접속을 허용하려면:

1. 공유기 관리 페이지 접속 (보통 192.168.0.1 또는 192.168.1.1)
2. **포트 포워딩** 또는 **가상 서버** 메뉴
3. 다음 규칙 추가:
   ```
   외부 포트: 22    → 내부 IP: 192.168.0.123, 포트: 22 (SSH)
   외부 포트: 8080  → 내부 IP: 192.168.0.123, 포트: 8080 (Spring Boot)
   ```

---

## 7. 프로젝트 클론 및 테스트

### 7.1 프로젝트 디렉토리 생성

```bash
# 홈 디렉토리에 프로젝트 폴더 생성
mkdir -p ~/projects
cd ~/projects
```

### 7.2 Git 저장소 클론

```bash
# GitHub에서 프로젝트 클론
git clone https://github.com/your-username/umc-9th-springboot-sweetheart.git
cd umc-9th-springboot-sweetheart

# 브랜치 확인
git branch -a
```

### 7.3 환경 변수 설정

```bash
# .env 파일 생성
nano .env
```

다음 내용 입력:

```env
# MySQL 설정
DB_URL=jdbc:mysql://mysql:3306/umc9th
DB_USER=umc_user
DB_PW=your_secure_password

# 스프링 프로필
SPRING_PROFILES_ACTIVE=prod
```

```bash
# 파일 권한 설정 (중요!)
chmod 600 .env

# Git에서 제외 확인
echo ".env" >> .gitignore
```

### 7.4 Docker Compose로 실행 테스트

```bash
# Docker Compose로 전체 스택 실행
docker compose up -d

# 로그 확인
docker compose logs -f backend

# 컨테이너 상태 확인
docker compose ps
```

출력 예시:
```
NAME                COMMAND                  SERVICE             STATUS              PORTS
umc9th-backend-1    "java -jar /app/..."     backend             running             0.0.0.0:8080->8080/tcp
umc9th-mysql-1      "docker-entrypoint..."   mysql               running             0.0.0.0:3306->3306/tcp
```

### 7.5 서비스 접근 확인

```bash
# Spring Boot API 확인
curl http://localhost:8080/actuator/health

# MySQL 접속 확인
docker compose exec mysql mysql -u umc_user -p umc9th
```

**Windows 데스크톱에서:**

```bash
# 맥미니 IP로 접근
curl http://192.168.0.123:8080/actuator/health
```

브라우저에서:
```
http://192.168.0.123:8080
```

---

## 8. 자동 시작 설정 (선택 사항)

### 8.1 맥미니 재부팅 시 Docker 자동 실행

```bash
# Docker Desktop 설정에서
# "Start Docker Desktop when you log in" 활성화

# 또는 LaunchDaemon 사용
sudo nano /Library/LaunchDaemons/com.umc.backend.plist
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.umc.backend</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/local/bin/docker</string>
        <string>compose</string>
        <string>up</string>
        <string>-d</string>
    </array>
    <key>WorkingDirectory</key>
    <string>/Users/your-username/projects/umc-9th-springboot-sweetheart</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <false/>
</dict>
</plist>
```

```bash
# LaunchDaemon 활성화
sudo launchctl load /Library/LaunchDaemons/com.umc.backend.plist
```

---

## 9. 트러블슈팅

### 9.1 Docker 실행 안 됨

```bash
# Docker Desktop이 실행 중인지 확인
ps aux | grep Docker

# Docker 재시작
killall Docker && open /Applications/Docker.app
```

### 9.2 포트 충돌

```bash
# 8080 포트 사용 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 <PID>
```

### 9.3 MySQL 연결 실패

```bash
# MySQL 컨테이너 로그 확인
docker compose logs mysql

# MySQL 컨테이너 재시작
docker compose restart mysql
```

### 9.4 디스크 공간 부족

```bash
# Docker 이미지 정리
docker system prune -a

# 사용하지 않는 볼륨 삭제
docker volume prune
```

---

## 10. 완료 체크리스트

- [ ] 고정 IP 설정 완료
- [ ] Homebrew 설치
- [ ] Java 21 설치
- [ ] Docker Desktop 설치
- [ ] SSH 접속 가능
- [ ] SSH 키 등록 완료
- [ ] 프로젝트 클론 완료
- [ ] Docker Compose로 실행 성공
- [ ] Windows에서 맥미니 API 접근 가능
- [ ] 방화벽 설정 완료

---

## 다음 단계

✅ 맥미니 설정 완료!

다음 문서를 참고하세요:
- `CLOUDFLARE_SETUP.md`: Cloudflare 터널로 HTTPS 도메인 연결
- `CICD_SETUP.md`: GitHub Actions 자동 배포 설정
- `DEPLOYMENT.md`: 전체 배포 프로세스 가이드
