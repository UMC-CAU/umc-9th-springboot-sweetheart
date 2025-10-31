# GitHub Actions CI/CD 설정 가이드

## 목표

`git push origin main`하면 자동으로 빌드 + 배포되는 시스템 구축

**완성 후:**
- ✅ 코드 푸시 → 자동 테스트
- ✅ 테스트 통과 → 자동 배포
- ✅ 맥미니에서 실행 (SSH 타임아웃 문제 해결)

---

## 1. Self-Hosted Runner 설정

### 1.1 GitHub에서 Runner 등록

1. https://github.com/[your-org]/[your-repo]
2. **Settings** → **Actions** → **Runners**
3. **New self-hosted runner** 클릭
4. **Runner image**: macOS 선택
5. **Architecture**: ARM64 선택
6. 화면에 나오는 **토큰 복사** (예: `ABCD1234...`)

### 1.2 맥미니에 Runner 설치

**SSH로 맥미니 접속:**
```bash
ssh [your_username]@[your_mac_mini_ip]
```

**Runner 설치:**
```bash
# actions-runner 폴더 생성
mkdir -p ~/actions-runner && cd ~/actions-runner

# Runner 다운로드 (Apple Silicon)
curl -o actions-runner-osx-arm64-2.321.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-osx-arm64-2.321.0.tar.gz

# 압축 해제
tar xzf ./actions-runner-osx-arm64-2.321.0.tar.gz

# Runner 등록 (GitHub에서 복사한 토큰 사용)
./config.sh --url https://github.com/[your-org]/[your-repo] --token [YOUR_TOKEN]
```

**설정 질문 (전부 Enter로 기본값):**
- Runner group: Enter
- Runner name: Enter (또는 `mac-mini-runner`)
- Work folder: Enter
- Labels: Enter

### 1.3 서비스로 등록 (자동 시작)

```bash
# 서비스 설치
./svc.sh install

# 서비스 시작
./svc.sh start

# 상태 확인
./svc.sh status
```

**성공 출력:**
```
status: Active: running
```

---

## 2. GitHub Actions 워크플로우 작성

### 2.1 CI 워크플로우 (테스트)

**파일:** `.github/workflows/ci.yml`

```yaml
name: CI - Test

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ develop, 'feature/**' ]

jobs:
  test:
    runs-on: ubuntu-latest

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

### 2.2 CD 워크플로우 (배포)

**파일:** `.github/workflows/deploy.yml`

```yaml
name: CD - Deploy to Mac Mini

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: self-hosted

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
    runs-on: self-hosted

    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Mac Mini
        run: |
          cd ~/projects/[your-repo]

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

---

## 3. 첫 배포 테스트

### 3.1 워크플로우 파일 커밋

```bash
# Windows에서
cd c:/projects/[your-repo]

git add .github/workflows/
git commit -m "feat: Add GitHub Actions CI/CD pipeline"
git push origin main
```

### 3.2 GitHub Actions 확인

1. https://github.com/[your-org]/[your-repo]/actions
2. 실행 중인 워크플로우 클릭
3. 실시간 로그 확인

**성공하면:**
- ✅ Build job 완료
- ✅ Deploy job 완료
- ✅ Health check 통과

---

## 완료 체크리스트

### Self-Hosted Runner
- [ ] GitHub에서 Runner 등록
- [ ] 맥미니에 Runner 설치
- [ ] 서비스로 등록 (자동 시작)
- [ ] 상태 확인 (running)

### GitHub Actions
- [ ] ci.yml 작성 (테스트)
- [ ] deploy.yml 작성 (배포)
- [ ] 워크플로우 파일 커밋
- [ ] 첫 배포 성공

### 확인
- [ ] GitHub Actions 로그 확인
- [ ] Health check 통과
- [ ] `git push origin main`으로 자동 배포 확인

---

## 다음 단계

✅ CI/CD 설정 완료!

**다음 작업:**
- [Cloudflare Tunnel 설정](CLOUDFLARE_SETUP.md) - HTTPS 외부 접속

---

## 트러블슈팅

### Runner가 안 보임
```bash
# 맥미니에서 상태 확인
cd ~/actions-runner
./svc.sh status

# 재시작
./svc.sh stop
./svc.sh start
```

### 배포 실패
```bash
# 맥미니에서 로그 확인
docker compose logs backend

# 수동 배포 테스트
cd ~/projects/[your-repo]
git pull origin main
docker compose up --build -d
```

### Health check 실패
```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs backend

# 재시작
docker compose restart
```
