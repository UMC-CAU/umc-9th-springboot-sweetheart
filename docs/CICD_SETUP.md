# GitHub Actions CI/CD 설정 가이드

## 목표

코드를 `main` 브랜치에 푸시하면 자동으로:
1. ✅ 테스트 실행
2. ✅ 빌드
3. ✅ 맥미니에 배포
4. ✅ Docker Compose로 재시작

## 목차

1. [배포 방법 선택](#1-배포-방법-선택) ⭐ 중요!
2. [Self-Hosted Runner 설정 (추천)](#2-self-hosted-runner-설정-추천)
3. [SSH 배포 방식 (문제 발생)](#3-ssh-배포-방식-문제-발생)
4. [CI/CD 파이프라인 개요 (참고)](#4-cicd-파이프라인-개요-참고)
5. [GitHub Secrets 설정 (참고)](#5-github-secrets-설정-참고)
6. [GitHub Actions 워크플로우 설명 (참고)](#6-github-actions-워크플로우-설명-참고)
7. [배포 스크립트 작성 (선택 사항)](#7-배포-스크립트-작성-선택-사항)
8. [첫 배포 테스트](#8-첫-배포-테스트)
9. [슬랙 알림 추가 (선택)](#9-슬랙-알림-추가-선택)
10. [트러블슈팅](#10-트러블슈팅)
11. [완료 체크리스트](#11-완료-체크리스트)
12. [배포 프로세스 요약](#12-배포-프로세스-요약)

---

## 1. 배포 방법 선택

### ⚠️ 현재 문제
Mac Mini는 로컬 네트워크(192.168.0.61) 안에만 있어서, GitHub Actions(클라우드)에서 직접 SSH 접속 불가능합니다.

```
❌ SSH 배포 실패 원인:
GitHub Actions (클라우드)
    ↓ SSH 시도
인터넷
    ↓
집 공유기
    ↓ 차단! (외부→내부 접속 불가)
Mac Mini (로컬 네트워크)
```

### 해결 방법 비교

| 방법 | 난이도 | 속도 | 보안 | 추천도 |
|------|--------|------|------|--------|
| **Self-Hosted Runner** | ⭐ 쉬움 | ⚡ 빠름 | 🔒 안전 | ⭐⭐⭐ |
| Cloudflare SSH Tunnel | ⭐⭐ 보통 | 🐢 보통 | 🔒 안전 | ⭐⭐ |
| 포트 포워딩 | ⭐ 쉬움 | ⚡ 빠름 | ⚠️ 위험 | ❌ 비추천 |

### 🎯 추천: Self-Hosted Runner

**장점:**
- ✅ 설정 5분이면 끝
- ✅ SSH 필요 없음 (로컬에서 실행)
- ✅ 가장 빠름
- ✅ 가장 안전

**단점:**
- Mac Mini가 꺼지면 배포 안 됨 (어차피 서버는 항상 켜져 있어야 함)

---

## 2. Self-Hosted Runner 설정 (추천)

### 2.1 GitHub에서 Runner 등록

1. **GitHub 저장소** 이동
   ```
   https://github.com/UMC-CAU/umc-9th-springboot-sweetheart
   ```

2. **Settings** → **Actions** → **Runners** 클릭

3. **New self-hosted runner** 클릭

4. **Runner image:** macOS 선택

5. **명령어가 표시됨** (복사하지 말고 다음 단계로)

### 2.2 Mac Mini에서 Runner 설치

**SSH로 Mac Mini 접속:**
```bash
ssh sweetheart@192.168.0.61
```

**Runner 다운로드 및 설정:**
```bash
# 홈 디렉토리에 actions-runner 폴더 생성
mkdir -p ~/actions-runner && cd ~/actions-runner

# Runner 다운로드 (Apple Silicon)
curl -o actions-runner-osx-arm64-2.321.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-osx-arm64-2.321.0.tar.gz

# 압축 해제
tar xzf ./actions-runner-osx-arm64-2.321.0.tar.gz

# Runner 설정
./config.sh --url https://github.com/UMC-CAU/umc-9th-springboot-sweetheart --token [GITHUB에서_제공한_토큰]
```

**설정 중 질문:**
- Runner group: **Enter** (기본값)
- Runner name: `mac-mini-runner` (또는 원하는 이름)
- Work folder: **Enter** (기본값: _work)
- Labels: **Enter** (기본값)

### 2.3 Runner 서비스로 등록 (자동 시작)

```bash
# Runner를 서비스로 설치
./svc.sh install

# 서비스 시작
./svc.sh start

# 상태 확인
./svc.sh status
```

### 2.4 deploy.yml 수정

Self-Hosted Runner용으로 수정 필요!

**파일 업데이트 필요:**
- `deploy.yml`을 Self-Hosted Runner용으로 수정 후 사용

---

## 3. SSH 배포 방식 (문제 발생)

### ⚠️ 현재 상태: SSH 타임아웃 에러

GitHub Actions(클라우드)에서 맥미니(로컬 네트워크)로 SSH 접속 시도 시 타임아웃 발생:

```
Error: dial tcp ***:22: i/o timeout
```

**원인:** 맥미니가 로컬 네트워크(192.168.0.61)에만 있어서 GitHub Actions 클라우드 서버에서 접근 불가

**해결 방법:**
1. ⭐ **Self-Hosted Runner 사용** (섹션 2 참고) - 추천!
2. Cloudflare Tunnel for SSH (복잡함, 비추천)
3. 포트 포워딩 (보안 위험, 비추천)

아래 내용은 SSH 배포가 정상 작동할 때를 위한 참고 자료입니다.

---

## 4. CI/CD 파이프라인 개요 (참고)

### 4.1 전체 워크플로우

```
[개발자] git push origin main
    ↓
[GitHub Actions 자동 트리거]
    ↓
┌─────────────────────────────────┐
│ 1. Test Stage                   │
│  - JUnit 테스트 실행            │
│  - 코드 빌드                     │
└─────────────────────────────────┘
    ↓ (성공 시)
┌─────────────────────────────────┐
│ 2. Build Stage                  │
│  - JAR 파일 생성                 │
│  - Docker 이미지 빌드           │
└─────────────────────────────────┘
    ↓ (성공 시)
┌─────────────────────────────────┐
│ 3. Deploy Stage                 │
│  - 맥미니 SSH 접속               │
│  - git pull                     │
│  - Docker Compose 재시작        │
└─────────────────────────────────┘
    ↓
[배포 완료] 🎉
[슬랙 알림] (선택)
```

### 4.2 파일 구조

```
umc-9th-springboot-sweetheart/
├── .github/
│   └── workflows/
│       ├── ci.yml              # 테스트만 (PR용)
│       └── deploy.yml          # 배포 (main 브랜치)
├── scripts/
│   └── deploy.sh               # 배포 스크립트 (선택)
├── Dockerfile
├── docker-compose.yml
└── .dockerignore
```

---

## 5. GitHub Secrets 설정 (참고)

### 5.1 SSH 키 생성 (Windows 데스크톱에서)

**이미 SSH 키가 있다면 스킵**

```bash
# SSH 키 생성
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/mac_mini_deploy

# 두 파일 생성됨:
# - mac_mini_deploy      (개인 키, GitHub Secret에 등록)
# - mac_mini_deploy.pub  (공개 키, 맥미니에 등록)

# 공개 키 확인
cat ~/.ssh/mac_mini_deploy.pub
```

출력 예시:
```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx github-actions
```

### 5.2 맥미니에 공개 키 등록

**맥미니 터미널에서:**

```bash
# SSH 디렉토리 생성 (없다면)
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# authorized_keys에 공개 키 추가
nano ~/.ssh/authorized_keys
# (위에서 복사한 공개 키 붙여넣기)

# 권한 설정
chmod 600 ~/.ssh/authorized_keys
```

**Windows에서 접속 테스트:**

```bash
ssh -i ~/.ssh/mac_mini_deploy your-username@192.168.0.123

# 비밀번호 없이 접속되면 성공!
```

### 5.3 GitHub Secrets 등록

#### 5.3.1 GitHub 저장소 설정 페이지로 이동

1. https://github.com/your-username/umc-9th-springboot-sweetheart
2. **Settings** 탭 클릭
3. 좌측 메뉴에서 **Secrets and variables** → **Actions** 클릭
4. **New repository secret** 버튼 클릭

#### 5.3.2 필수 Secrets 등록

##### **MAC_MINI_HOST**

```
Name: MAC_MINI_HOST
Value: 192.168.0.123
```

(맥미니의 **로컬 네트워크** IP 주소)

**주의:** Cloudflare 터널을 사용하더라도, GitHub Actions는 로컬 네트워크에서 SSH 접속합니다!

##### **MAC_MINI_USER**

```
Name: MAC_MINI_USER
Value: your-username
```

(맥미니 사용자명)

##### **MAC_MINI_SSH_KEY**

```bash
# Windows에서 개인 키 내용 복사
cat ~/.ssh/mac_mini_deploy
```

출력 전체를 복사:
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
...
-----END OPENSSH PRIVATE KEY-----
```

```
Name: MAC_MINI_SSH_KEY
Value: (위에서 복사한 개인 키 전체)
```

**중요:**
- `-----BEGIN`부터 `-----END`까지 전체 복사
- 줄바꿈 포함해서 그대로 붙여넣기

##### **DB_PASSWORD** (선택)

```
Name: DB_PASSWORD
Value: your_mysql_password
```

GitHub Actions 워크플로우에서 환경 변수로 사용할 수 있습니다.

#### 5.3.3 Secrets 확인

등록 후 다음과 같이 표시됨:

```
MAC_MINI_HOST         Updated now
MAC_MINI_USER         Updated now
MAC_MINI_SSH_KEY      Updated now
DB_PASSWORD           Updated now
```

**주의:** Secret 값은 다시 볼 수 없습니다. 수정만 가능합니다.

---

## 6. GitHub Actions 워크플로우 설명 (참고)

### 6.1 CI 워크플로우 (`.github/workflows/ci.yml`)

**용도:** Pull Request에서 테스트만 실행 (배포 안 함)

**트리거:**
- Pull Request가 `main` 또는 `develop` 브랜치로 생성될 때
- `develop` 또는 `feature/**` 브랜치에 푸시할 때

**주요 단계:**
1. 코드 체크아웃
2. Java 21 설치
3. 테스트 실행 (`./gradlew test`)
4. 빌드 (`./gradlew build`)
5. 테스트 결과 업로드

**파일 내용 설명:**

```yaml
on:
  pull_request:
    branches: [ main, develop ]  # PR 생성 시
  push:
    branches: [ develop, 'feature/**' ]  # 브랜치에 푸시 시
```

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: 'gradle'  # Gradle 캐시로 빌드 속도 향상
```

```yaml
- name: Upload test results
  if: always()  # 테스트 실패해도 결과 업로드
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: build/reports/tests/
```

### 6.2 CD 워크플로우 (`.github/workflows/deploy.yml`)

**용도:** `main` 브랜치에 푸시하면 자동 배포

**트리거:**
- `main` 브랜치에 푸시할 때

**주요 단계:**

#### **Job 1: test**
1. 코드 체크아웃
2. Java 21 설치
3. 테스트 실행
4. 빌드

#### **Job 2: deploy** (test 성공 시에만 실행)
1. 맥미니 SSH 접속
2. `git pull origin main`
3. `docker compose down`
4. `docker compose up --build -d`
5. Health check (`/actuator/health`)

**파일 내용 설명:**

```yaml
deploy:
  needs: test  # test job이 성공해야만 실행
  runs-on: ubuntu-latest
```

```yaml
- name: Deploy to Mac Mini
  uses: appleboy/ssh-action@master  # SSH 접속 액션
  with:
    host: ${{ secrets.MAC_MINI_HOST }}
    username: ${{ secrets.MAC_MINI_USER }}
    key: ${{ secrets.MAC_MINI_SSH_KEY }}
    script: |
      # 맥미니에서 실행될 명령어들
```

```bash
# Health check
if curl -f http://localhost:8080/actuator/health; then
  echo "✅ Deployment successful!"
else
  echo "❌ Deployment failed"
  docker compose logs backend
  exit 1  # 워크플로우 실패 처리
fi
```

---

## 7. 배포 스크립트 작성 (선택 사항)

### 7.1 deploy.sh 스크립트 생성 (맥미니에서)

복잡한 배포 로직을 별도 스크립트로 분리:

```bash
# 맥미니에서
mkdir -p ~/projects/umc-9th-springboot-sweetheart/scripts
nano ~/projects/umc-9th-springboot-sweetheart/scripts/deploy.sh
```

**scripts/deploy.sh:**

```bash
#!/bin/bash

set -e  # 에러 발생 시 즉시 종료

PROJECT_DIR=~/projects/umc-9th-springboot-sweetheart
LOG_FILE=$PROJECT_DIR/deploy.log

cd $PROJECT_DIR

echo "========================================" | tee -a $LOG_FILE
echo "🚀 Deployment started at $(date)" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE

# 1. Git pull
echo "📥 Pulling latest code..." | tee -a $LOG_FILE
git pull origin main | tee -a $LOG_FILE

# 2. Docker Compose down
echo "🛑 Stopping existing containers..." | tee -a $LOG_FILE
docker compose down | tee -a $LOG_FILE

# 3. Docker Compose up
echo "🐳 Building and starting containers..." | tee -a $LOG_FILE
docker compose up --build -d | tee -a $LOG_FILE

# 4. Wait for services
echo "⏳ Waiting for services to start..." | tee -a $LOG_FILE
sleep 15

# 5. Health check
echo "✅ Checking service health..." | tee -a $LOG_FILE
MAX_RETRIES=5
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Health check passed!" | tee -a $LOG_FILE
    docker compose ps | tee -a $LOG_FILE
    echo "========================================" | tee -a $LOG_FILE
    echo "🎉 Deployment successful at $(date)" | tee -a $LOG_FILE
    echo "========================================" | tee -a $LOG_FILE
    exit 0
  fi

  RETRY_COUNT=$((RETRY_COUNT + 1))
  echo "⏳ Retry $RETRY_COUNT/$MAX_RETRIES..." | tee -a $LOG_FILE
  sleep 5
done

# Health check failed
echo "❌ Health check failed after $MAX_RETRIES retries" | tee -a $LOG_FILE
echo "📋 Container logs:" | tee -a $LOG_FILE
docker compose logs backend | tail -n 50 | tee -a $LOG_FILE
exit 1
```

```bash
# 실행 권한 부여
chmod +x ~/projects/umc-9th-springboot-sweetheart/scripts/deploy.sh
```

### 7.2 GitHub Actions에서 스크립트 사용

`deploy.yml` 수정:

```yaml
- name: Deploy to Mac Mini
  uses: appleboy/ssh-action@master
  with:
    host: ${{ secrets.MAC_MINI_HOST }}
    username: ${{ secrets.MAC_MINI_USER }}
    key: ${{ secrets.MAC_MINI_SSH_KEY }}
    script: |
      cd ~/projects/umc-9th-springboot-sweetheart
      ./scripts/deploy.sh
```

훨씬 간결해졌습니다!

---

## 8. 첫 배포 테스트

### 8.1 로컬에서 변경 사항 커밋

**Windows 데스크톱 터미널에서:**

```bash
cd c:/projects/UMC/umc-9th-springboot-sweetheart

# 워크플로우 파일 커밋
git add .github/workflows/
git commit -m "feat: Add GitHub Actions CI/CD pipeline"

# main 브랜치에 푸시
git push origin main
```

### 8.2 GitHub Actions 모니터링

1. https://github.com/your-username/umc-9th-springboot-sweetheart/actions
2. 방금 푸시한 커밋에 대한 워크플로우 실행 확인
3. **CD - Deploy to Mac Mini** 클릭
4. 실시간 로그 확인

**진행 상황:**
```
✅ test / test
  - Checkout code
  - Set up JDK 21
  - Run tests
  - Build with Gradle

✅ deploy / deploy
  - Checkout code
  - Deploy to Mac Mini
    - Pulling latest code...
    - Rebuilding Docker containers...
    - Checking service health...
    - ✅ Deployment successful!
```

### 8.3 배포 확인

**브라우저에서:**
```
https://spring-swagger-api.log8.kr/actuator/health
```

**Windows 터미널에서:**
```bash
curl https://spring-swagger-api.log8.kr/actuator/health
```

출력:
```json
{"status":"UP"}
```

### 8.4 배포 로그 확인 (맥미니)

```bash
# 맥미니에서
cd ~/projects/umc-9th-springboot-sweetheart

# 배포 로그 (deploy.sh 사용 시)
cat deploy.log

# Docker 로그
docker compose logs backend
```

---

## 9. 슬랙 알림 추가 (선택)

### 9.1 Slack Webhook URL 생성

1. Slack 워크스페이스에서 **Apps** → **Incoming Webhooks** 검색
2. **Add to Slack** 클릭
3. 알림을 받을 채널 선택
4. **Webhook URL** 복사

```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
```

### 9.2 GitHub Secret 등록

```
Name: SLACK_WEBHOOK_URL
Value: (위에서 복사한 Webhook URL)
```

### 9.3 deploy.yml에 슬랙 알림 추가

```yaml
jobs:
  test:
    # ... (기존 내용)

  deploy:
    needs: test
    runs-on: ubuntu-latest

    steps:
      # ... (기존 배포 단계)

      - name: Slack Notification - Success
        if: success()
        uses: 8398a7/action-slack@v3
        with:
          status: custom
          custom_payload: |
            {
              "text": "✅ Deployment Successful!",
              "attachments": [{
                "color": "good",
                "fields": [
                  {
                    "title": "Repository",
                    "value": "${{ github.repository }}",
                    "short": true
                  },
                  {
                    "title": "Branch",
                    "value": "${{ github.ref }}",
                    "short": true
                  },
                  {
                    "title": "Commit",
                    "value": "${{ github.event.head_commit.message }}",
                    "short": false
                  },
                  {
                    "title": "URL",
                    "value": "https://spring-swagger-api.log8.kr",
                    "short": false
                  }
                ]
              }]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}

      - name: Slack Notification - Failure
        if: failure()
        uses: 8398a7/action-slack@v3
        with:
          status: custom
          custom_payload: |
            {
              "text": "❌ Deployment Failed!",
              "attachments": [{
                "color": "danger",
                "fields": [
                  {
                    "title": "Repository",
                    "value": "${{ github.repository }}",
                    "short": true
                  },
                  {
                    "title": "Branch",
                    "value": "${{ github.ref }}",
                    "short": true
                  },
                  {
                    "title": "Error",
                    "value": "Check GitHub Actions for details",
                    "short": false
                  }
                ]
              }]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 10. 트러블슈팅

### 10.1 SSH 연결 실패 (타임아웃)

**에러 메시지:**
```
err: dial tcp 192.168.0.123:22: connect: connection refused
```

**원인:**
- 맥미니 SSH 서버가 비활성화됨
- IP 주소 변경됨
- 네트워크 연결 문제

**해결:**

```bash
# 맥미니에서 SSH 활성화 확인
sudo systemsetup -getremotelogin

# SSH 활성화
sudo systemsetup -setremotelogin on

# IP 주소 확인
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows에서 SSH 연결 테스트
ssh -i ~/.ssh/mac_mini_deploy your-username@192.168.0.123
```

### 10.2 Permission denied (publickey)

**에러 메시지:**
```
Permission denied (publickey).
```

**원인:**
- 공개 키가 맥미니에 등록되지 않음
- GitHub Secret의 개인 키가 잘못됨

**해결:**

```bash
# 맥미니에서 authorized_keys 확인
cat ~/.ssh/authorized_keys

# 권한 확인
ls -la ~/.ssh/
# drwx------ (700)
ls -la ~/.ssh/authorized_keys
# -rw------- (600)

# 권한 수정
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### 10.3 Health check failed

**에러 메시지:**
```
❌ Deployment failed - health check failed
```

**원인:**
- Spring Boot 앱이 정상적으로 시작되지 않음
- 데이터베이스 연결 실패
- 포트 충돌

**해결:**

```bash
# 맥미니에서 로그 확인
docker compose logs backend

# 컨테이너 상태 확인
docker compose ps

# MySQL 연결 확인
docker compose logs mysql

# 포트 확인
lsof -i :8080
```

### 10.4 Git pull 실패

**에러 메시지:**
```
error: Your local changes to the following files would be overwritten by merge
```

**원인:**
- 맥미니에서 코드를 직접 수정함

**해결:**

```bash
# 맥미니에서
cd ~/projects/umc-9th-springboot-sweetheart

# 로컬 변경 사항 확인
git status

# 로컬 변경 사항 되돌리기
git reset --hard origin/main

# 다시 배포
./scripts/deploy.sh
```

### 10.5 Docker build 실패

**에러 메시지:**
```
ERROR: failed to solve: process "/bin/sh -c ./gradlew build" did not complete
```

**원인:**
- Dockerfile의 빌드 명령어 오류
- 메모리 부족

**해결:**

```bash
# 맥미니에서 Docker 메모리 확인
docker info | grep Memory

# Docker Desktop 설정에서 메모리 증가
# Docker Desktop → Preferences → Resources → Memory: 4GB 이상

# 수동 빌드 테스트
cd ~/projects/umc-9th-springboot-sweetheart
docker compose build backend
```

---

## 11. 완료 체크리스트

### Self-Hosted Runner 방식 (추천)
- [x] GitHub에서 Runner 등록
- [x] 맥미니에 Runner 설치 및 설정
- [x] Runner 서비스로 등록 (자동 시작)
- [x] deploy.yml을 Self-Hosted Runner용으로 수정
- [x] 첫 배포 테스트 성공 🎉
- [x] Health check 통과

### SSH 방식 (참고, 현재 타임아웃 발생 중)
- [x] SSH 키 생성 및 맥미니에 등록
- [x] GitHub Secrets 등록 완료
- [x] `.github/workflows/ci.yml` 작성
- [x] `.github/workflows/deploy.yml` 작성
- [ ] SSH 타임아웃 문제 해결 필요

### 공통
- [ ] Cloudflare Tunnel 설정 (HTTPS 외부 접속용) - 다음 단계!
- [ ] 슬랙 알림 설정 (선택)
- [x] 배포 로그 확인

---

## 12. 배포 프로세스 요약

### 개발 워크플로우

```
1. 로컬에서 코드 작성
   ↓
2. feature 브랜치에 커밋
   git commit -m "feat: Add new feature"
   git push origin feature/new-feature
   ↓
3. Pull Request 생성
   → GitHub Actions CI 자동 실행 (테스트만)
   ↓
4. 코드 리뷰 후 main 브랜치에 머지
   ↓
5. GitHub Actions CD 자동 실행
   - 테스트
   - 빌드
   - 맥미니 배포
   ↓
6. 배포 완료!
   https://spring-swagger-api.log8.kr
```

### 배포 시간

- **테스트**: 약 2-3분
- **빌드**: 약 1-2분
- **배포**: 약 1-2분
- **총**: 약 4-7분

---

## 다음 단계

✅ CI/CD 설정 완료!

다음 문서를 참고하세요:
- `DEPLOYMENT.md`: 전체 배포 프로세스 총정리
- `MAC_MINI_SETUP.md`: 맥미니 추가 설정
- `CLOUDFLARE_SETUP.md`: Cloudflare 고급 설정
