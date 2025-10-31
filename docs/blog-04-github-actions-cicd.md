---
title: "git push만으로 자동 배포 - Self-Hosted Runner로 CI/CD 완성하기 🚀"
description: "SSH 타임아웃의 벽을 넘어, 맥미니에서 직접 돌아가는 GitHub Actions로 완전 자동화 배포 구축하기"
pubDate: 2025-01-18
author: "SweetHeart"
tags: ["GitHub Actions", "CI/CD", "Self-Hosted Runner", "DevOps", "자동배포", "Docker"]
series: "맥미니 홈서버 구축기"
seriesOrder: 4
heroImage: "/blog/mac-mini-server/04-hero.jpg"
---

# git push만으로 자동 배포 - Self-Hosted Runner로 CI/CD 완성하기 🚀

## 현재 상황: 수동 배포의 고통

코드를 수정할 때마다:

```bash
# 1. Windows PC에서 코드 작성
git add .
git commit -m "feat: Add new feature"
git push origin main

# 2. 맥미니 SSH 접속
ssh sweetheart@192.168.0.61

# 3. 코드 업데이트
cd ~/projects/umc-9th-springboot-sweetheart
git pull origin main

# 4. Docker 재배포
docker compose down
docker compose up --build -d

# 5. 로그 확인 (혹시 에러?)
docker compose logs -f backend
```

**매번 5단계!** 😫

이걸 **자동화**하고 싶어요!

## 목표: 완전 자동화 배포

```
┌────────────────────────────────────────────┐
│  개발자 PC                                  │
│  git push origin main                      │
└────────────────────────────────────────────┘
          ↓ (자동!)
┌────────────────────────────────────────────┐
│  GitHub Actions                            │
│  ✅ 테스트 실행                            │
│  ✅ 빌드                                   │
│  ✅ 배포                                   │
└────────────────────────────────────────────┘
          ↓ (자동!)
┌────────────────────────────────────────────┐
│  맥미니 홈서버                              │
│  ✅ git pull                               │
│  ✅ docker compose up --build -d          │
│  ✅ 배포 완료!                             │
└────────────────────────────────────────────┘
```

**git push 한 번으로 끝!** 🎉

## 첫 시도: GitHub Actions + SSH

### 워크플로우 파일 작성

`.github/workflows/deploy.yml`

```yaml
name: Deploy to Mac Mini

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Deploy via SSH
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.MAC_MINI_HOST }}
          username: ${{ secrets.MAC_MINI_USER }}
          key: ${{ secrets.MAC_MINI_SSH_KEY }}
          script: |
            cd ~/projects/umc-9th-springboot-sweetheart
            git pull origin main
            docker compose down
            docker compose up --build -d
```

### GitHub Secrets 설정

**GitHub 저장소** → **Settings** → **Secrets and variables** → **Actions**

```
MAC_MINI_HOST = 192.168.0.61
MAC_MINI_USER = sweetheart
MAC_MINI_SSH_KEY = (SSH private key)
```

### SSH 키 생성

Windows PC에서:

```bash
ssh-keygen -t ed25519 -C "github-actions"
```

출력:
```
Generating public/private ed25519 key pair.
Enter file in which to save the key: C:\Users\SweetHeart\.ssh\mac_mini_deploy
```

**공개 키를 맥미니에 등록:**

```bash
# 공개 키 내용 복사
cat C:\Users\SweetHeart\.ssh\mac_mini_deploy.pub

# 맥미니에서
ssh sweetheart@192.168.0.61
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5..." >> ~/.ssh/authorized_keys
```

### git push 테스트!

```bash
git add .
git commit -m "feat: Setup CI/CD pipeline"
git push origin main
```

GitHub Actions 페이지 확인:
https://github.com/[your-org]/[your-repo]/actions

워크플로우 실행 시작... 기대에 부푼 마음으로 지켜봅니다... 🤞

## 참담한 실패: SSH 타임아웃 😱

### 에러 로그

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

### 원인 분석

```
┌─────────────────────────────────────────┐
│  GitHub Actions (Cloud)                 │
│  IP: 13.124.xxx.xxx (서울 리전)         │
└─────────────────────────────────────────┘
          ↓ SSH 접속 시도
          ↓ (192.168.0.61:22)
          ✖ 타임아웃!
┌─────────────────────────────────────────┐
│  맥미니 (로컬 네트워크)                  │
│  IP: 192.168.0.61                       │
│  (공인 IP 없음, 포트 포워딩 없음)        │
└─────────────────────────────────────────┘
```

**문제:**
- 맥미니는 **로컬 네트워크(192.168.0.x)**에만 있음
- GitHub Actions는 **클라우드(AWS)에서 실행**
- 클라우드 → 로컬 네트워크 접속 불가! 🚫

### 실패한 해결 시도들

#### 1️⃣ 공인 IP + 포트 포워딩?

```
❌ 보안 위험 (SSH 포트 개방)
❌ 유동 IP (재부팅 시 변경)
❌ Cloudflare Tunnel로 HTTP만 터널링 가능 (SSH는 별도 설정)
```

#### 2️⃣ Cloudflare Tunnel for SSH?

```
❌ 추가 설정 복잡함
❌ SSH 터널 안정성 문제
```

#### 3️⃣ ngrok 같은 터널?

```
❌ 유료 (SSH 터널링은 Pro 플랜)
❌ 의존성 추가
```

**좌절...** 😔

그러다 발견한 해결책!

## 해결책: Self-Hosted Runner! 🏠

### Self-Hosted Runner란?

**GitHub Actions를 맥미니에서 직접 실행!**

```
┌─────────────────────────────────────────┐
│  GitHub (클라우드)                       │
│  코드 푸시 감지 → 워크플로우 트리거      │
└─────────────────────────────────────────┘
          ↓ 작업 요청
┌─────────────────────────────────────────┐
│  맥미니 (Self-Hosted Runner)             │
│  ~/actions-runner/run.sh                │
│  ✅ 작업 수신                            │
│  ✅ 테스트 실행                          │
│  ✅ 빌드                                 │
│  ✅ 배포 (로컬에서!)                     │
└─────────────────────────────────────────┘
```

### 장점

- ✅ **SSH 불필요** (로컬에서 직접 실행)
- ✅ **빠름** (네트워크 지연 없음)
- ✅ **무료** (GitHub 무료 플랜도 사용 가능)
- ✅ **완전한 제어** (루트 권한, 환경 설정 자유)

### 단점

- ⚠️ **물리 서버 관리** (맥미니가 꺼지면 작동 안 됨)
- ⚠️ **보안** (GitHub Actions가 맥미니에 접근)

토이 프로젝트에는 **완벽한 선택!** 🎯

## Self-Hosted Runner 설치

### 1. GitHub에서 Runner 등록

**GitHub 저장소** → **Settings** → **Actions** → **Runners**

**New self-hosted runner** 클릭

선택 사항:
- Runner image: **macOS**
- Architecture: **ARM64** (M1/M2)

**토큰 복사!** (예: `ABCD1234EFGH5678...`)

⚠️ 토큰은 브라우저 닫으면 다시 못 봅니다!

### 2. 맥미니에 Runner 다운로드

```bash
ssh sweetheart@192.168.0.61
```

Runner 디렉토리 생성:

```bash
mkdir -p ~/actions-runner && cd ~/actions-runner
```

Runner 다운로드:

```bash
curl -o actions-runner-osx-arm64-2.321.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-osx-arm64-2.321.0.tar.gz
```

압축 해제:

```bash
tar xzf ./actions-runner-osx-arm64-2.321.0.tar.gz
```

### 3. Runner 설정

```bash
./config.sh --url https://github.com/[your-org]/[your-repo] --token [YOUR_TOKEN]
```

질문 답변:
```
Enter the name of the runner group: [Enter] (Default로)
Enter the name of the runner: [Enter] (또는 'mac-mini-runner')
Enter any additional labels: [Enter]
Enter name of work folder: [Enter] (_work로)
```

출력:
```
√ Runner successfully added
√ Runner connection is good
```

완료! ✅

### 4. 서비스로 등록 (자동 시작)

```bash
./svc.sh install
```

출력:
```
Creating launch runner in /Users/sweetheart/Library/LaunchAgents/actions.runner.xxx.plist
```

서비스 시작:

```bash
./svc.sh start
```

### 5. 상태 확인

```bash
./svc.sh status
```

출력:
```
status:
/Users/sweetheart/Library/LaunchAgents/actions.runner.xxx.plist
Started:
7458 0 actions.runner.xxx
```

**Active: running!** ✅

### 6. GitHub에서 확인

**Settings** → **Actions** → **Runners**

```
┌────────────────────────────────────────┐
│  mac-mini-runner                       │
│  Status: Idle 🟢                       │
│  Labels: self-hosted, macOS, ARM64     │
└────────────────────────────────────────┘
```

**Idle 상태!** 대기 중이네요! 🎉

## 워크플로우 수정

### deploy.yml 수정

이제 `runs-on: ubuntu-latest`를 `runs-on: self-hosted`로 변경!

`.github/workflows/deploy.yml`

```yaml
name: CD - Deploy to Mac Mini

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: self-hosted  # ✨ 변경!

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build (skip tests)
        run: ./gradlew build -x test

  deploy:
    needs: build
    runs-on: self-hosted  # ✨ 변경!

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Mac Mini
        run: |
          cd ~/projects/umc-9th-springboot-sweetheart

          echo "📥 Pulling latest code..."
          git pull origin main

          echo "🐳 Rebuilding Docker containers..."
          docker compose down
          docker compose up --build -d

          echo "⏳ Waiting for services..."
          sleep 10

          echo "✅ Checking health..."
          docker compose ps

          if curl -f http://localhost:8080/actuator/health; then
            echo "✅ Deployment successful!"
          else
            echo "❌ Deployment failed"
            docker compose logs backend
            exit 1
          fi
```

### 핵심 변경사항

1. **runs-on: self-hosted** - 맥미니에서 실행
2. **SSH 제거** - 직접 실행이니까 불필요!
3. **로컬 경로 사용** - `~/projects/...`
4. **Health check 추가** - 배포 성공 여부 확인

## 첫 자동 배포 테스트! 🚀

### Windows PC에서 코드 수정

```bash
cd c:/projects/UMC/umc-9th-springboot-sweetheart
```

간단한 변경:

```java
// src/main/java/.../controller/TestController.java
@GetMapping("/hello")
public String hello() {
    return "Hello from Mac Mini! Auto-deployed! 🚀";
}
```

### git push!

```bash
git add .
git commit -m "feat: Add hello endpoint for auto-deploy test"
git push origin main
```

**이제 지켜보기만 하면 됩니다!** 🍿

### GitHub Actions 페이지

https://github.com/[your-org]/[your-repo]/actions

워크플로우 실행 시작...

```
🔄 CD - Deploy to Mac Mini
   ├─ build (self-hosted)  ⏳ Running...
   │  ├─ Set up job        ✅ 5s
   │  ├─ Checkout          ✅ 3s
   │  ├─ Set up JDK 21     ✅ 12s
   │  ├─ Build             ⏳ Running...
```

빌드 로그:
```
> Task :bootJar
> Task :build

BUILD SUCCESSFUL in 1m 23s
```

**빌드 성공!** ✅

```
   └─ deploy (self-hosted)  ⏳ Running...
      ├─ Checkout           ✅ 2s
      ├─ Deploy to Mac Mini ⏳ Running...
```

배포 로그:
```
📥 Pulling latest code...
Already up to date.

🐳 Rebuilding Docker containers...
[+] Running 2/2
 ✔ Container umc-backend  Started
 ✔ Container umc-mysql    Started

⏳ Waiting for services...

✅ Checking health...
NAME            STATUS    PORTS
umc-backend     Up        0.0.0.0:8080->8080/tcp
umc-mysql       Up        0.0.0.0:3306->3306/tcp

{"status":"UP"}
✅ Deployment successful!
```

**전부 초록불!** 🟢🟢🟢

### 실제 접속 테스트

```bash
curl https://spring-swagger-api.log8.kr/hello
```

응답:
```
Hello from Mac Mini! Auto-deployed! 🚀
```

**완벽하게 작동합니다!** 🎉🎉🎉

## 전체 배포 시간 측정 ⏱️

```
┌─────────────────────────────────────┐
│  git push (Windows PC)              │
│  0초                                │
└─────────────────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│  GitHub 워크플로우 트리거            │
│  + 3초                              │
└─────────────────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│  Build (Gradle)                     │
│  + 1분 23초                         │
└─────────────────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│  Deploy (Docker Compose)            │
│  + 2분 15초                         │
└─────────────────────────────────────┘
          ↓
┌─────────────────────────────────────┐
│  Health Check                       │
│  + 10초                             │
└─────────────────────────────────────┘

총 소요 시간: 약 4분
```

**git push 후 4분이면 배포 완료!** 🚀

## CI 워크플로우 추가 (Pull Request용)

배포 전에 테스트도 자동화합시다!

`.github/workflows/ci.yml`

```yaml
name: CI - Test

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ develop, 'feature/**' ]

jobs:
  test:
    runs-on: ubuntu-latest  # PR 테스트는 GitHub 호스팅 사용 (빠름)

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run tests
        run: ./gradlew test

      - name: Build
        run: ./gradlew build
```

### 개발 워크플로우

```
┌────────────────────────────────────────┐
│  feature/new-feature 브랜치 작업        │
│  git push origin feature/new-feature   │
└────────────────────────────────────────┘
          ↓
┌────────────────────────────────────────┐
│  CI: Test (ubuntu-latest)              │
│  ✅ 테스트 실행                        │
│  ✅ 빌드 확인                          │
└────────────────────────────────────────┘
          ↓ PR 생성
┌────────────────────────────────────────┐
│  코드 리뷰                              │
└────────────────────────────────────────┘
          ↓ Merge to main
┌────────────────────────────────────────┐
│  CD: Deploy (self-hosted)              │
│  ✅ 빌드                               │
│  ✅ 배포                               │
└────────────────────────────────────────┘
```

## 배포 로그 실시간 확인

### 맥미니에서 Runner 로그 확인

```bash
cd ~/actions-runner
tail -f _diag/Runner_*.log
```

실시간으로 워크플로우 실행 로그를 볼 수 있어요!

### Docker 로그 확인

```bash
docker compose logs -f backend
```

배포 후 Spring Boot 로그 실시간 확인!

## 비용 비교: GitHub Actions Minutes

### GitHub 호스팅 (ubuntu-latest)

```
GitHub Free 계정:
- 2,000분/월 무료 (Public 저장소는 무제한)

Private 저장소 초과 시:
- Linux: $0.008/분 (약 ₩10)
- macOS: $0.08/분 (약 ₩100) 😱
```

### Self-Hosted Runner

```
초기 비용: ₩0 (맥미니 이미 보유)
실행 비용: ₩0 (무제한!)
전기세: ₩0 추가 (이미 켜져 있음)

💰 완전 무료!
```

**월 100회 배포해도 무료!** 🎉

## 보안 고려사항 🔒

### Runner 보안

Self-Hosted Runner는 **코드 실행 권한**이 있습니다!

⚠️ **주의:**
- Public 저장소에는 사용 금지! (누구나 PR 보낼 수 있음)
- Private 저장소에만 사용
- 팀원만 접근 가능하게 설정

### 권장 설정

1. **GitHub 저장소를 Private으로**
2. **Branch Protection Rules 설정**
   - main 브랜치 직접 푸시 금지
   - PR + 리뷰 필수
3. **Secrets 관리**
   - .env 파일은 절대 커밋 금지
   - GitHub Secrets 사용

## 다음 편 예고

이제 완전 자동화 배포가 완성되었습니다! 🎉

```
✅ git push → 자동 빌드 → 자동 배포
✅ PR → 자동 테스트
✅ HTTPS 외부 접속
✅ 무료!
```

하지만... 여기까지 오는 동안 **수많은 삽질**이 있었죠! 😅

다음 편에서는:
- 🔧 **Cloudflare Status 1 에러** (새벽 2시 삽질)
- 🔧 **SSH 키 비밀번호 문제** (Shift 키 오작동)
- 🔧 **MySQL 컨테이너 연결 실패**
- 🔧 **Docker 메모리 부족**
- 🔧 **각종 타임아웃과의 전쟁**

실전 트러블슈팅 모음집!

> **5편: 삽질 기록 - 트러블슈팅 모음집 🔧** (Coming Soon)

---

## 마치며

드디어 **완전 자동화 배포**를 달성했습니다!

```bash
git push origin main
```

이 한 줄이면 끝! 🚀

맥미니 홈서버로 이런 프로덕션급 CI/CD를 무료로 구축할 수 있다니 감격입니다. 😭✨

여러분도 Self-Hosted Runner 써보세요!
댓글로 경험 공유해주세요! 💬

---

## 시리즈 목차

1. AWS 요금 폭탄 💸에서 맥미니 홈서버로 탈출하기
2. 맥미니 개봉부터 첫 배포까지 🖥️
3. 포트 포워딩 없이 HTTPS 열기 - Cloudflare Tunnel 🔒
4. **git push만으로 자동 배포 - Self-Hosted Runner 🚀** ← 현재
5. 삽질 기록 - 트러블슈팅 모음집 🔧
6. 맥미니 홈서버 1개월 후기 & 최종 정산 💰

---

**Tags:** #GitHubActions #CICD #SelfHostedRunner #DevOps #자동배포 #Docker #홈서버
