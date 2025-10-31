# 맥미니 초기 설정 가이드

## 목표

맥미니를 Spring Boot 배포 서버로 설정하기

**필요한 것:**
- Mac Mini (Apple Silicon 권장)
- 모니터 + 키보드 (초기 설정용, 나중에 제거 가능)
- 같은 Wi-Fi 네트워크

---

## 1. 맥미니 기본 설정

### 1.1 초기 설정 (모니터 연결)

1. 모니터, 키보드 연결
2. 전원 켜기
3. macOS 설정:
   - **계정 이름**: `[your_username]`
   - **비밀번호**: 간단하게 (예: `password123`)
   - **힌트**: 기억하기 쉬운 것

### 1.2 네트워크 확인

```bash
# 터미널 열기 (Cmd+Space → "터미널" 검색)
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**출력 예시:**
```
inet 192.168.0.123
```

이 IP 주소를 기억해두세요!

### 1.3 고정 IP 설정

1. **시스템 설정** → **네트워크**
2. 사용 중인 네트워크 → **세부사항**
3. **TCP/IP** 탭
4. **IPv4 구성**: `수동으로` 선택
5. 설정:
   - **IPv4 주소**: `192.168.0.123` (위에서 확인한 IP)
   - **서브넷 마스크**: `255.255.255.0`
   - **라우터**: `192.168.0.1` (공유기 주소)
6. **DNS** 탭:
   - `8.8.8.8`
   - `8.8.4.4`
7. **확인** 클릭

---

## 2. SSH 활성화

### 2.1 원격 로그인 켜기

1. **시스템 설정** → **공유**
2. **원격 로그인** 켜기
3. **접근 허용**: `모든 사용자` 선택

### 2.2 접속 테스트 (Windows에서)

```bash
ssh [your_username]@192.168.0.123
# 비밀번호 입력
```

성공하면 이제 모니터 없이도 작업 가능!

---

## 3. 필수 소프트웨어 설치

### 3.1 Homebrew 설치

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# PATH 추가 (M1/M2 Mac)
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
source ~/.zprofile
```

### 3.2 Java 21 설치

```bash
brew install openjdk@21

# 확인
java -version
```

### 3.3 Docker Desktop 설치

```bash
brew install --cask docker
```

**설치 후:**
1. Finder → Applications → Docker 실행
2. 로그인 (Docker Hub 계정)
3. 환경설정:
   - **Start Docker Desktop when you log in** ✅ 체크

---

## 4. 프로젝트 설정

### 4.1 Git 설정

```bash
# Git 사용자 정보
git config --global user.name "[Your Name]"
git config --global user.email "[your-email@example.com]"

# 프로젝트 클론
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/[your-org]/[your-repo].git
cd [your-repo]
```

### 4.2 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env
nano .env
```

**설정 예시:**
```env
DB_ROOT_PASSWORD=[your_db_root_password]
DB_USER=[your_db_user]
DB_PW=[your_db_password]
SPRING_PROFILES_ACTIVE=prod
DDL_AUTO=update
SHOW_SQL=true
```

### 4.3 Docker Compose 실행

```bash
# 컨테이너 시작
docker compose up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f
```

### 4.4 테스트

```bash
# Health check
curl http://localhost:8080/actuator/health
```

**성공 출력:**
```json
{"status":"UP"}
```

---

## 5. Chrome Remote Desktop 설정 (선택)

모니터 없이 GUI 접근하려면:

1. 맥미니에서 Chrome 설치
2. https://remotedesktop.google.com/access
3. "이 기기 설정" 클릭
4. 지시 따라 설정

이제 Windows에서 Chrome Remote Desktop으로 접속 가능!

---

## 6. 자동 로그인 설정 (선택)

맥미니 재부팅 시 자동 로그인:

1. **시스템 설정** → **사용자 및 그룹**
2. **자동 로그인**: 사용자 선택
3. 비밀번호 입력

---

## 완료 체크리스트

### 기본 설정
- [ ] macOS 초기 설정 완료
- [ ] 고정 IP 설정
- [ ] SSH 원격 로그인 활성화
- [ ] Windows에서 SSH 접속 확인

### 소프트웨어
- [ ] Homebrew 설치
- [ ] Java 21 설치
- [ ] Docker Desktop 설치 및 자동 시작 설정

### 프로젝트 설정
- [ ] Git 설정
- [ ] 프로젝트 클론
- [ ] .env 파일 생성
- [ ] Docker Compose 실행
- [ ] Health check 성공

### 접근성 (선택)
- [ ] Chrome Remote Desktop 설정
- [ ] 자동 로그인 설정

---

## 다음 단계

✅ 맥미니 기본 설정 완료!

**다음 작업:**
1. [Self-Hosted Runner 설정](CICD_SETUP.md) - 자동 배포
2. [Cloudflare Tunnel 설정](CLOUDFLARE_SETUP.md) - HTTPS 외부 접속

---

## 트러블슈팅

### SSH 접속 안 됨
```bash
# 맥미니에서 SSH 상태 확인
sudo systemsetup -getremotelogin

# SSH 활성화
sudo systemsetup -setremotelogin on
```

### Docker 실행 안 됨
- Docker Desktop이 실행 중인지 확인
- 재시작: Applications → Docker → Restart

### Health check 실패
```bash
# 로그 확인
docker compose logs backend

# 컨테이너 재시작
docker compose restart
```
