# 전체 배포 가이드

## 🎯 목표

맥미니 + Docker + Cloudflare + GitHub Actions (Self-Hosted Runner)를 활용한 완전 자동화 배포 시스템 구축

**최종 결과:**
- 🔒 HTTPS: `https://spring-swagger-api.log8.kr`
- 🚀 자동 배포: `git push origin main`만으로 배포 완료
- 📊 모니터링: Swagger UI, Health Check, 로그

**⚠️ 현재 상태:**
- ✅ 맥미니 설정 완료
- ✅ Docker + MySQL 실행 중
- ✅ Cloudflare Tunnel 설정 완료 (HTTPS 외부 접속 가능)
- ✅ GitHub Actions CI/CD 워크플로우 작성 완료
- ⚠️ **SSH 배포 타임아웃 발생 중** (맥미니가 로컬 네트워크에만 있음)
- 🔨 **해결 방법: Self-Hosted Runner 설정 필요** (CICD_SETUP.md 섹션 2 참고)

---

## 📋 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      Development Flow                        │
└─────────────────────────────────────────────────────────────┘

[개발자] 코드 작성 → git push origin main
    ↓
[GitHub] 코드 푸시 감지
    ↓
[Mac Mini - Self-Hosted Runner] ~/actions-runner
    ├─ GitHub Actions 워크플로우 실행
    ├─ Test (JUnit)
    ├─ Build (Gradle)
    └─ Deploy
        ↓
        ~/projects/umc-9th-springboot-sweetheart
        ├─ git pull
        ├─ docker compose up --build -d
        │   ├─ MySQL Container (3306)
        │   └─ Spring Boot Container (8080)
    ↓
[Cloudflare Tunnel] cloudflared
    ├─ localhost:8080 → Cloudflare Edge
    └─ HTTPS 자동 처리
    ↓
[사용자] https://spring-swagger-api.log8.kr
```

---

## 🗂️ 문서 구조

이 저장소에는 다음 문서들이 있습니다:

1. **MAC_MINI_SETUP.md** - 맥미니 초기 설정
   - Java, Docker, MySQL 설치
   - 네트워크 설정 (고정 IP)
   - SSH 설정

2. **CLOUDFLARE_SETUP.md** - Cloudflare 터널 설정
   - cloudflared 설치
   - 터널 생성 및 DNS 설정
   - HTTPS 자동 인증서

3. **CICD_SETUP.md** - GitHub Actions CI/CD
   - GitHub Secrets 설정
   - 자동 테스트 및 배포
   - 슬랙 알림

4. **DEPLOYMENT.md** (이 문서)
   - 전체 배포 프로세스 총정리
   - 빠른 시작 가이드

---

## 🚀 빠른 시작 (Quick Start)

### 단계별 체크리스트

#### ✅ Phase 1: 맥미니 설정 (30분)

**참고 문서:** `docs/MAC_MINI_SETUP.md`

```bash
# 맥미니에서 실행

# 1. Homebrew 설치
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Java 21 설치
brew install openjdk@21

# 3. Docker Desktop 설치
brew install --cask docker
# Docker Desktop 실행

# 4. 고정 IP 설정
# 시스템 설정 → 네트워크 → TCP/IP → 수동 설정

# 5. SSH 활성화
# 시스템 설정 → 공유 → 원격 로그인 활성화

# 6. 프로젝트 클론
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/your-username/umc-9th-springboot-sweetheart.git
cd umc-9th-springboot-sweetheart

# 7. 환경 변수 설정
cp .env.example .env
nano .env  # 비밀번호 수정

# 8. Docker Compose 테스트
docker compose up -d
curl http://localhost:8080/actuator/health
```

---

#### ✅ Phase 2: Cloudflare 터널 설정 (20분)

**참고 문서:** `docs/CLOUDFLARE_SETUP.md`

```bash
# 맥미니에서 실행

# 1. cloudflared 설치
brew install cloudflare/cloudflare/cloudflared

# 2. Cloudflare 로그인
cloudflared tunnel login

# 3. 터널 생성
cloudflared tunnel create mac-mini-umc

# 4. 설정 파일 생성
mkdir -p ~/.cloudflared
nano ~/.cloudflared/config.yml
```

**config.yml 내용:**
```yaml
tunnel: mac-mini-umc
credentials-file: /Users/your-username/.cloudflared/xxxxxxxx.json

ingress:
  - hostname: spring-swagger-api.log8.kr
    service: http://localhost:8080
  - service: http_status:404
```

```bash
# 5. DNS 라우팅
cloudflared tunnel route dns mac-mini-umc spring-swagger-api.log8.kr

# 6. 터널 서비스 등록
sudo cloudflared service install
sudo launchctl start com.cloudflare.cloudflared

# 7. 테스트
curl https://spring-swagger-api.log8.kr/actuator/health
```

---

#### ✅ Phase 3: GitHub Actions CI/CD 설정 (15분)

**참고 문서:** `docs/CICD_SETUP.md` (섹션 1-2 참고)

**⚠️ 현재 상태:**
- SSH 배포 방식은 타임아웃 발생 (맥미니가 로컬 네트워크에만 있음)
- **Self-Hosted Runner 설정 필요** (5분이면 완료)

**1. GitHub에서 Runner 등록**

1. GitHub 저장소 → Settings → Actions → Runners
2. New self-hosted runner 클릭
3. macOS 선택
4. 표시되는 토큰 복사

**2. 맥미니에 Runner 설치**

```bash
# 맥미니 SSH 접속
ssh sweetheart@192.168.0.61

# Runner 다운로드 및 설치
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-osx-arm64-2.321.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-osx-arm64-2.321.0.tar.gz
tar xzf ./actions-runner-osx-arm64-2.321.0.tar.gz

# Runner 설정 (GitHub에서 제공한 토큰 사용)
./config.sh --url https://github.com/UMC-CAU/umc-9th-springboot-sweetheart --token [GITHUB_TOKEN]

# 서비스로 등록 (자동 시작)
./svc.sh install
./svc.sh start
./svc.sh status
```

**3. deploy.yml 수정**

Self-Hosted Runner용으로 수정 필요 (runs-on: self-hosted)

**4. 첫 배포 테스트**
```bash
# Windows 데스크톱에서
cd c:/projects/UMC/umc-9th-springboot-sweetheart
git add .
git commit -m "feat: Setup Self-Hosted Runner deployment"
git push origin main

# GitHub Actions 확인
# https://github.com/UMC-CAU/umc-9th-springboot-sweetheart/actions
```

**참고: SSH 방식 (현재 타임아웃)**
- GitHub Secrets (MAC_MINI_HOST, MAC_MINI_USER, MAC_MINI_SSH_KEY) 이미 등록됨 ✅
- `.github/workflows/ci.yml` 작성 완료 ✅
- `.github/workflows/deploy.yml` 작성 완료 ✅
- SSH 접속 타임아웃으로 Self-Hosted Runner로 전환 필요 ⚠️

---

## 📂 프로젝트 파일 구조

```
umc-9th-springboot-sweetheart/
│
├── docs/                           # 📚 모든 배포 문서
│   ├── MAC_MINI_SETUP.md
│   ├── CLOUDFLARE_SETUP.md
│   ├── CICD_SETUP.md
│   └── DEPLOYMENT.md
│
├── .github/
│   └── workflows/
│       ├── ci.yml                  # PR 테스트
│       └── deploy.yml              # 자동 배포
│
├── scripts/
│   └── deploy.sh                   # 배포 스크립트
│
├── src/                            # Spring Boot 소스 코드
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│
├── Dockerfile                      # Docker 이미지 빌드
├── docker-compose.yml              # 멀티 컨테이너 설정
├── .dockerignore
├── .env.example                    # 환경 변수 템플릿
├── .gitignore
├── build.gradle
└── README.md
```

---

## 🔄 배포 워크플로우

### 1. 개발 → 배포 프로세스

```
┌─────────────────────────────────────────────────────────────┐
│  1. 로컬 개발 (Windows Desktop)                             │
└─────────────────────────────────────────────────────────────┘
  코드 작성 → 테스트 → 커밋

┌─────────────────────────────────────────────────────────────┐
│  2. Feature 브랜치 푸시                                      │
└─────────────────────────────────────────────────────────────┘
  git checkout -b feature/new-feature
  git commit -m "feat: Add new feature"
  git push origin feature/new-feature

┌─────────────────────────────────────────────────────────────┐
│  3. Pull Request 생성                                       │
└─────────────────────────────────────────────────────────────┘
  → GitHub Actions CI 실행 (자동)
    ✅ 테스트
    ✅ 빌드

┌─────────────────────────────────────────────────────────────┐
│  4. 코드 리뷰 & Merge to main                               │
└─────────────────────────────────────────────────────────────┘
  → GitHub Actions CD 실행 (자동)
    ✅ 테스트
    ✅ 빌드
    ✅ 맥미니 SSH 접속
    ✅ Docker Compose 재배포

┌─────────────────────────────────────────────────────────────┐
│  5. 배포 완료!                                               │
└─────────────────────────────────────────────────────────────┘
  https://spring-swagger-api.log8.kr
```

### 2. 배포 시간

| 단계 | 소요 시간 |
|------|-----------|
| 테스트 실행 | 2-3분 |
| 빌드 | 1-2분 |
| Docker 이미지 빌드 | 2-3분 |
| 컨테이너 재시작 | 1분 |
| **총 소요 시간** | **6-9분** |

---

## 🛠️ 수동 배포 (긴급 상황)

### 맥미니에서 직접 배포

```bash
# 1. 맥미니 SSH 접속 (Windows에서)
ssh your-username@192.168.0.123

# 2. 프로젝트 디렉토리 이동
cd ~/projects/umc-9th-springboot-sweetheart

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

### 배포 스크립트 사용 (권장)

```bash
# 맥미니에서
cd ~/projects/umc-9th-springboot-sweetheart
./scripts/deploy.sh
```

---

## 🔍 모니터링 및 확인

### 1. 서비스 상태 확인

```bash
# 맥미니에서

# 컨테이너 상태
docker compose ps

# 로그 확인
docker compose logs -f backend    # 실시간
docker compose logs backend | tail -n 100  # 최근 100줄

# Health check
curl http://localhost:8080/actuator/health
```

### 2. 외부 접속 확인

```bash
# Windows에서

# Health check
curl https://spring-swagger-api.log8.kr/actuator/health

# Swagger UI
# 브라우저: https://spring-swagger-api.log8.kr/swagger-ui.html
```

### 3. Cloudflare 터널 상태

```bash
# 맥미니에서
sudo launchctl list | grep cloudflare

# 터널 로그
sudo tail -f /var/log/cloudflared.log
```

### 4. 데이터베이스 확인

```bash
# 맥미니에서
docker compose exec mysql mysql -u umc_user -p umc9th

# MySQL CLI
SHOW TABLES;
SELECT * FROM member;
```

---

## 🐛 트러블슈팅

### 문제 1: 배포 후 502 Bad Gateway

**증상:**
```
https://spring-swagger-api.log8.kr → 502 Bad Gateway
```

**원인:**
- Spring Boot 앱이 정상적으로 시작되지 않음

**해결:**
```bash
# 맥미니에서
docker compose logs backend | tail -n 100

# 일반적인 원인:
# 1. MySQL 연결 실패 → DB_PASSWORD 확인
# 2. 포트 충돌 → lsof -i :8080
# 3. 메모리 부족 → docker stats
```

### 문제 2: GitHub Actions 배포 실패 (SSH 타임아웃)

**증상:**
```
err: dial tcp ***:22: i/o timeout
```

**원인:**
- 맥미니가 로컬 네트워크(192.168.0.61)에만 있어서 GitHub Actions 클라우드에서 접근 불가

**해결 (추천):**
```bash
# Self-Hosted Runner 설정 (CICD_SETUP.md 섹션 2 참고)
# 맥미니에서 직접 GitHub Actions 실행

cd ~/actions-runner
./config.sh --url https://github.com/UMC-CAU/umc-9th-springboot-sweetheart --token [TOKEN]
./svc.sh install
./svc.sh start
```

**대안 (복잡함, 비추천):**
- Cloudflare Tunnel for SSH
- 포트 포워딩 (보안 위험)

### 문제 3: Cloudflare 터널 연결 안 됨

**증상:**
```
Cloudflare Tunnel status: Disconnected
```

**해결:**
```bash
# 맥미니에서

# 1. 터널 서비스 상태 확인
sudo launchctl list | grep cloudflare

# 2. 터널 재시작
sudo launchctl stop com.cloudflare.cloudflared
sudo launchctl start com.cloudflare.cloudflared

# 3. 설정 파일 확인
cat ~/.cloudflared/config.yml

# 4. 수동 실행 테스트
cloudflared tunnel run mac-mini-umc
```

### 문제 4: Docker 빌드 실패

**증상:**
```
ERROR: failed to solve: process "/bin/sh -c ./gradlew build" did not complete
```

**해결:**
```bash
# 맥미니에서

# 1. 로컬에서 빌드 테스트
./gradlew clean build

# 2. Docker 메모리 증가
# Docker Desktop → Preferences → Resources → Memory: 4GB

# 3. 빌드 캐시 정리
docker builder prune -a
```

---

## 📊 유용한 명령어 모음

### Docker 관련

```bash
# 전체 재시작
docker compose restart

# 특정 서비스만 재시작
docker compose restart backend

# 로그 실시간 확인
docker compose logs -f

# 컨테이너 내부 접속
docker compose exec backend bash
docker compose exec mysql bash

# 리소스 사용량 확인
docker stats

# 디스크 정리
docker system prune -a
docker volume prune
```

### Git 관련

```bash
# 최신 코드 가져오기
git fetch origin
git pull origin main

# 로컬 변경 사항 되돌리기
git reset --hard origin/main

# 브랜치 확인
git branch -a

# 최근 커밋 확인
git log --oneline -n 10
```

### 시스템 관련

```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :3306

# 프로세스 종료
kill -9 <PID>

# 디스크 사용량
df -h

# 메모리 사용량
top
```

---

## 🔐 보안 고려사항

### 1. 환경 변수 관리

```bash
# .env 파일은 절대 Git에 커밋하지 않기
echo ".env" >> .gitignore

# GitHub Secrets 사용
# 민감한 정보는 모두 Secrets에 저장
```

### 2. SSH 키 관리

```bash
# 개인 키 권한 설정
chmod 600 ~/.ssh/mac_mini_deploy

# 공개 키만 서버에 등록
# 개인 키는 로컬에만 보관
```

### 3. 방화벽 설정

```bash
# 맥미니에서 불필요한 포트 차단
# Cloudflare Tunnel 사용 시 8080 포트 외부 노출 불필요
```

### 4. Cloudflare Access Policy (선택)

- Swagger UI를 공개하고 싶지 않다면
- Cloudflare Access로 이메일 인증 추가
- 특정 사용자만 접근 허용

---

## 📈 다음 단계

### Phase 4: 추가 기능

1. **모니터링 추가**
   - Prometheus + Grafana
   - Spring Boot Actuator 메트릭
   - 알람 설정

2. **로깅 중앙화**
   - ELK Stack (Elasticsearch, Logstash, Kibana)
   - 로그 수집 및 분석

3. **데이터베이스 백업**
   - 자동 백업 스크립트
   - S3 또는 외부 스토리지 연동

4. **Blue-Green 배포**
   - 무중단 배포
   - 트래픽 전환

5. **부하 테스트**
   - JMeter 또는 Gatling
   - 성능 최적화

---

## 🎓 학습 자료

### 공식 문서
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Docker](https://docs.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)
- [GitHub Actions](https://docs.github.com/en/actions)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)

### 관련 개념
- CI/CD Pipeline
- Containerization
- Infrastructure as Code
- Zero Trust Network Access

---

## 📞 문의 및 지원

### 문제 발생 시

1. **로그 확인**: `docker compose logs backend`
2. **Health check**: `curl http://localhost:8080/actuator/health`
3. **GitHub Issues**: 저장소에 이슈 등록
4. **Cloudflare Dashboard**: 터널 상태 확인

---

## ✅ 최종 체크리스트

### 맥미니 설정
- [x] Java 21 설치
- [x] Docker Desktop 설치
- [x] MySQL 컨테이너 실행
- [x] SSH 접속 가능
- [x] 고정 IP 설정
- [x] 프로젝트 클론 완료
- [x] Chrome Remote Desktop 설정
- [x] 자동 로그인 설정

### Cloudflare 설정
- [ ] cloudflared 설치 ⭐ 다음 단계!
- [ ] 터널 생성
- [ ] DNS 라우팅 설정
- [ ] HTTPS 접속 확인
- [ ] 서비스 자동 시작 설정

### CI/CD 설정
- [x] SSH 키 생성 및 등록
- [x] GitHub Secrets 등록
- [x] 워크플로우 파일 작성 (ci.yml, deploy.yml)
- [x] **Self-Hosted Runner 설정** ✨
- [x] deploy.yml을 Self-Hosted Runner용으로 수정
- [x] 첫 배포 성공 🎉
- [x] Health check 통과

### 최종 확인
- [ ] `https://spring-swagger-api.log8.kr` 접속 가능
- [ ] Swagger UI 확인
- [ ] API 호출 테스트
- [x] `git push origin main`으로 자동 배포 확인 ✨
- [x] 로그 모니터링 설정

---

## 🎉 현재 진행 상황

**완료된 작업:**
- ✅ **로컬 개발 환경**: Windows 데스크톱
- ✅ **배포 서버**: 맥미니 (Docker + MySQL 실행 중)
- ✅ **Self-Hosted Runner**: 자동 배포 완료 🎉
- ✅ **CI/CD 워크플로우**: `git push origin main`으로 자동 배포됨!
- ✅ **원격 접속**: SSH, Chrome Remote Desktop

**다음 단계 (약 10분):**
- 🔨 **Cloudflare Tunnel 설정** ([CLOUDFLARE_SETUP.md](CLOUDFLARE_SETUP.md) 참고)
- 🔨 **HTTPS 외부 접속**: `https://spring-swagger-api.log8.kr`
- 🔒 **Zero Trust Access Policy** (선택, 보안 강화)

완료하면 전 세계 어디서나 HTTPS로 API 접근 가능!

**Happy Coding! 🚀**
