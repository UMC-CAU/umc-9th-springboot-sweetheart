# 전체 배포 가이드

## 목표

맥미니 + Docker + Cloudflare + GitHub Actions (Self-Hosted Runner) 완전 자동화 배포

**최종 결과:**
- ✅ HTTPS 외부 접속: `https://[your-subdomain].[your-domain]`
- ✅ 자동 배포: `git push origin main` → 자동 빌드 + 배포
- ✅ 무료!

---

## 배포 아키텍처

```
[개발자 PC]
    ↓ git push origin main
[GitHub]
    ↓ 워크플로우 실행
[맥미니 - Self-Hosted Runner]
    ├─ Build + Test
    ├─ Docker Compose 재배포
    │   ├─ MySQL Container
    │   └─ Spring Boot Container (localhost:8080)
    ↓
[Cloudflare Tunnel]
    ↓ HTTPS
[외부 사용자] → https://[your-subdomain].[your-domain]
```

---

## 빠른 시작 (Quick Start)

### 준비물

- Mac Mini (Apple Silicon 권장)
- 도메인 (Cloudflare에 등록)
- GitHub 저장소

### 1단계: 맥미니 설정 (30분)

**문서:** [MAC_MINI_SETUP.md](MAC_MINI_SETUP.md)

**요약:**
```bash
# SSH로 맥미니 접속
ssh [your_username]@[your_mac_mini_ip]

# Homebrew, Java 21, Docker Desktop 설치
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install openjdk@21
brew install --cask docker

# 프로젝트 클론
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/[your-org]/[your-repo].git
cd [your-repo]

# 환경 변수 설정
cp .env.example .env
nano .env  # 비밀번호 수정

# Docker Compose 실행
docker compose up -d
curl http://localhost:8080/actuator/health
```

**체크리스트:**
- [ ] Java 21 설치
- [ ] Docker Desktop 설치
- [ ] 고정 IP 설정
- [ ] SSH 활성화
- [ ] 프로젝트 클론
- [ ] .env 파일 생성
- [ ] Docker Compose 실행 성공

---

### 2단계: Cloudflare Tunnel 설정 (20분)

**문서:** [CLOUDFLARE_SETUP.md](CLOUDFLARE_SETUP.md)

**요약:**
```bash
# cloudflared 설치
brew install cloudflare/cloudflare/cloudflared

# 로그인 및 터널 생성
cloudflared tunnel login
cloudflared tunnel create [tunnel-name]

# 설정 파일 생성 (/etc/cloudflared/config.yml)
sudo mkdir -p /etc/cloudflared
sudo cp ~/.cloudflared/[tunnel-id].json /etc/cloudflared/
# config.yml 작성 (문서 참고)

# DNS 라우팅
cloudflared tunnel route dns [tunnel-name] [your-subdomain].[your-domain]

# 자동 시작 설정 (plist 파일 생성)
# /Library/LaunchDaemons/com.cloudflare.cloudflared.plist 생성
sudo launchctl load /Library/LaunchDaemons/com.cloudflare.cloudflared.plist

# 테스트
curl https://[your-subdomain].[your-domain]/actuator/health
```

**체크리스트:**
- [ ] cloudflared 설치
- [ ] 터널 생성
- [ ] config.yml 작성
- [ ] DNS 라우팅 설정
- [ ] plist 파일 생성 (ProgramArguments 완전히 작성!)
- [ ] HTTPS 접속 성공

---

### 3단계: GitHub Actions CI/CD 설정 (15분)

**문서:** [CICD_SETUP.md](CICD_SETUP.md)

**요약:**

**1) GitHub에서 Runner 등록**
1. GitHub 저장소 → Settings → Actions → Runners
2. New self-hosted runner → macOS 선택
3. 토큰 복사

**2) 맥미니에 Runner 설치**
```bash
# 맥미니에서
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-osx-arm64-2.321.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-osx-arm64-2.321.0.tar.gz
tar xzf ./actions-runner-osx-arm64-2.321.0.tar.gz

# Runner 설정
./config.sh --url https://github.com/[your-org]/[your-repo] --token [YOUR_TOKEN]

# 서비스로 등록
./svc.sh install
./svc.sh start
./svc.sh status
```

**3) 워크플로우 파일 작성**
- `.github/workflows/ci.yml` (테스트용)
- `.github/workflows/deploy.yml` (배포용, runs-on: self-hosted)

**4) 첫 배포**
```bash
# Windows PC에서
git add .
git commit -m "feat: Setup CI/CD pipeline"
git push origin main

# GitHub Actions 페이지에서 확인
```

**체크리스트:**
- [ ] Self-Hosted Runner 설치
- [ ] 서비스 자동 시작 설정
- [ ] ci.yml 작성
- [ ] deploy.yml 작성 (runs-on: self-hosted)
- [ ] 첫 배포 성공

---

## 수동 배포 (긴급 상황)

```bash
# 1. 맥미니 SSH 접속
ssh [your_username]@[your_mac_mini_ip]

# 2. 프로젝트 디렉토리 이동
cd ~/projects/[your-repo]

# 3. 최신 코드 가져오기
git pull origin main

# 4. Docker Compose 재배포
docker compose down
docker compose up --build -d

# 5. 로그 확인
docker compose logs -f backend

# 6. Health check
curl http://localhost:8080/actuator/health
```

---

## 모니터링

### 서비스 상태 확인

```bash
# 맥미니에서

# 컨테이너 상태
docker compose ps

# 로그 확인
docker compose logs -f backend

# Health check
curl http://localhost:8080/actuator/health

# Cloudflare Tunnel 상태
sudo launchctl list | grep cloudflare
```

### 외부 접속 확인

```bash
# Windows PC에서

# Health check
curl https://[your-subdomain].[your-domain]/actuator/health

# Swagger UI
# 브라우저: https://[your-subdomain].[your-domain]/swagger-ui.html
```

---

## 트러블슈팅

### 1. 502 Bad Gateway

```bash
# Spring Boot 앱 확인
docker compose ps
docker compose logs backend

# 재시작
docker compose restart backend
```

### 2. GitHub Actions 배포 실패

```bash
# Self-Hosted Runner 상태 확인
cd ~/actions-runner
./svc.sh status

# 재시작
./svc.sh stop
./svc.sh start
```

### 3. Cloudflare Tunnel 연결 안 됨

```bash
# 서비스 상태 확인
sudo launchctl list | grep cloudflare

# Status가 1이면 plist 파일 확인
cat /Library/LaunchDaemons/com.cloudflare.cloudflared.plist

# ProgramArguments가 완전한지 확인:
# - /opt/homebrew/bin/cloudflared
# - --config
# - /etc/cloudflared/config.yml
# - tunnel
# - run
# - [tunnel-name]

# 재시작
sudo launchctl unload /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
sudo launchctl load /Library/LaunchDaemons/com.cloudflare.cloudflared.plist
```

### 4. MySQL 연결 실패

```bash
# .env 파일 확인
cat .env

# MySQL 컨테이너 상태
docker compose ps mysql
docker compose logs mysql

# 재시작
docker compose restart mysql
```

---

## 유용한 명령어

### Docker

```bash
# 전체 재시작
docker compose restart

# 로그 실시간 확인
docker compose logs -f

# 컨테이너 내부 접속
docker compose exec backend bash
docker compose exec mysql bash

# 디스크 정리
docker system prune -a
```

### Git

```bash
# 최신 코드 가져오기
git pull origin main

# 로컬 변경 사항 되돌리기
git reset --hard origin/main
```

### 시스템

```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :3306

# 디스크 사용량
df -h

# 메모리 사용량
top
```

---

## 완료 체크리스트

### 전체 시스템
- [ ] **맥미니 설정 완료** (MAC_MINI_SETUP.md)
- [ ] **Cloudflare Tunnel 설정 완료** (CLOUDFLARE_SETUP.md)
- [ ] **GitHub Actions CI/CD 설정 완료** (CICD_SETUP.md)

### 최종 확인
- [ ] `https://[your-subdomain].[your-domain]` 접속 가능
- [ ] Swagger UI 확인
- [ ] `git push origin main`으로 자동 배포 확인
- [ ] Health check 통과

---

## 다음 단계 (선택 사항)

### 보안 강화
- Zero Trust Access Policy 설정
- API 인증 추가
- 방화벽 설정

### 모니터링
- Prometheus + Grafana
- 슬랙 알림
- 로그 중앙화 (ELK Stack)

### 성능 최적화
- 데이터베이스 인덱싱
- 캐싱 (Redis)
- 부하 테스트

---

## 문서 구조

```
docs/
├── MAC_MINI_SETUP.md      # 1단계: 맥미니 초기 설정
├── CLOUDFLARE_SETUP.md    # 2단계: Cloudflare 터널 설정
├── CICD_SETUP.md          # 3단계: GitHub Actions 설정
└── DEPLOYMENT.md          # 전체 가이드 (이 문서)
```

각 문서를 순서대로 따라하면 완전 자동화 배포 시스템 완성!
