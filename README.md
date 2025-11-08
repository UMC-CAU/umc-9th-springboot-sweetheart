# 🍰 UMC 9th SpringBoot Project

> University MakeUs Challenge 9기 Spring Boot 학습 프로젝트

**미션 기반 맛집 리뷰 플랫폼** - 가게별 미션을 완료하고 포인트를 획득하며, 리뷰를 작성하는 서비스입니다.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

---

## 📋 목차

- [기술 스택](#-기술-스택)
- [주요 기능](#-주요-기능)
- [프로젝트 구조](#-프로젝트-구조)
- [ERD 보기](#-erd-보기)
- [시작하기](#-시작하기)
- [API 문서](#-api-문서)
- [개발 가이드](#-개발-가이드)

---

## 🛠 기술 스택

### Backend
- **Java 21** - LTS 버전
- **Spring Boot 3.5.6** - 최신 Spring Framework
- **Spring Data JPA** - ORM 및 데이터 접근
- **QueryDSL** - 타입 안전 쿼리

### Database
- **MySQL 8.0** - 메인 데이터베이스
- **H2 Database** - 테스트용 인메모리 DB

### Tools
- **Gradle** - 빌드 도구
- **Lombok** - 보일러플레이트 코드 제거
- **Springdoc OpenAPI** - API 문서 자동 생성

### DevOps
- **GitHub Actions** - CI/CD
- **Cloudflare Tunnel** - 안전한 배포
- **Mac Mini** - 자체 호스팅 서버

---

## 💡 주요 기능

### 👤 회원 관리
- 소셜 로그인 지원 (Kakao, Naver, Apple, Google)
- 선호 음식 카테고리 설정
- 포인트 시스템
- 휴대폰 인증 (선택)

### 🏪 가게 관리
- 지역별 가게 검색
- 음식 카테고리별 분류
- 가게별 미션 및 리뷰 조회

### 🎯 미션 시스템
- 가게별 미션 생성
- 미션 진행 상태 관리 (진행가능/진행중/완료)
- 미션 완료 시 포인트 지급

### ⭐ 리뷰 시스템
- 가게 리뷰 작성 및 별점
- 사진 다중 첨부
- 리뷰 댓글 (사장님 답글)

---

## 📁 프로젝트 구조

```
src/main/java/com/example/umc9th/
├── domain/                 # 도메인별 모듈
│   ├── member/            # 회원 도메인
│   │   ├── entity/        # Member, Food, Term 엔티티
│   │   ├── repository/    # JPA Repository
│   │   ├── service/       # 비즈니스 로직
│   │   ├── controller/    # REST API
│   │   └── dto/           # DTO
│   ├── store/             # 가게 도메인
│   ├── mission/           # 미션 도메인
│   ├── review/            # 리뷰 도메인
│   └── location/          # 지역 도메인
│
├── global/                # 공통 모듈
│   ├── entity/            # BaseEntity, BaseTimeEntity
│   ├── response/          # 통합 API Response
│   ├── exception/         # 전역 예외 처리
│   └── config/            # 설정 파일
│
└── UMC9thApplication.java # Main Application
```

### 아키텍처 특징

✅ **Domain-Driven Design** - 도메인별 모듈화
✅ **Layered Architecture** - Entity → Repository → Service → Controller
✅ **N+1 문제 해결** - Fetch Join 전략 적용
✅ **통합 API Response** - 일관된 응답 구조
✅ **전역 예외 처리** - @RestControllerAdvice

---

## 📊 ERD 보기

프로젝트의 데이터베이스 구조를 확인하려면:

### 🎨 온라인에서 시각화 (추천)

1. **[dbdiagram.io](https://dbdiagram.io/d)** 접속
2. [`docs/ERD.dbml`](./docs/ERD.dbml) 파일 내용 복사
3. 에디터에 붙여넣기
4. 자동으로 ERD 렌더링! ✨

### ⚙️ 추천 설정

dbdiagram.io에서 이렇게 설정하면 가장 보기 좋습니다:

- **Font Size**: Large (큰 글씨)
- **Connection Style**: Orthogonal (직각 선)
- **Theme**: Light 또는 Dark (취향껏)

### 💾 내보내기

- **PNG**: 고해상도 이미지 (발표, 보고서용)
- **PDF**: 인쇄용
- **SQL**: MySQL, PostgreSQL 등 DDL 자동 생성

### 📝 ERD 업데이트

엔티티 수정 시 [`docs/ERD.dbml`](./docs/ERD.dbml) 파일도 함께 업데이트해주세요!

```dbml
Table member {
  id bigint [pk]
  name varchar(50)
  nickname varchar(50)  // ← 필드 추가 예시
  ...
}
```

---

## 🚀 시작하기

### 사전 요구사항

- **Java 21** 이상
- **MySQL 8.0** 이상
- **Gradle** (또는 래퍼 사용)

### 1️⃣ 프로젝트 클론

```bash
git clone https://github.com/UMC-CAU/umc-9th-springboot-sweetheart.git
cd umc-9th-springboot-sweetheart
```

### 2️⃣ 데이터베이스 설정

MySQL에서 데이터베이스를 생성하세요:

```sql
CREATE DATABASE umc9th CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3️⃣ 환경 변수 설정

`application.yml`에 데이터베이스 연결 정보 입력:

```yaml
spring:
  datasource:
    url: ${DB_URL}      # jdbc:mysql://localhost:3306/umc9th
    username: ${DB_USER}
    password: ${DB_PW}
```

또는 환경 변수로 설정:

```bash
export DB_URL=jdbc:mysql://localhost:3306/umc9th
export DB_USER=root
export DB_PW=your_password
```

### 4️⃣ 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/umc9th-0.0.1-SNAPSHOT.jar
```

### 5️⃣ 접속 확인

브라우저에서 다음 주소로 접속:

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

---

## 📖 API 문서

### Swagger UI

애플리케이션 실행 후 http://localhost:8080/swagger-ui.html 에서 모든 API를 테스트할 수 있습니다.

### API Response 구조

모든 API는 통일된 응답 구조를 사용합니다:

**성공 응답:**
```json
{
  "isSuccess": true,
  "code": "MEMBER_200",
  "message": "회원 조회 성공",
  "timestamp": "2024-01-15T10:30:00",
  "data": { ... }
}
```

**에러 응답:**
```json
{
  "isSuccess": false,
  "code": "MEMBER_404",
  "message": "회원을 찾을 수 없습니다",
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/members/999",
  "traceId": "abc-123-def"
}
```

---

## 👨‍💻 개발 가이드

### 빌드 명령어

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "com.example.umc9th.ClassName"

# 빌드 캐시 삭제
./gradlew clean
```

### 코드 컨벤션

자세한 개발 가이드는 [`CLAUDE.md`](./CLAUDE.md)를 참조하세요.

**주요 규칙:**

1. **Entity 작성**
   - `BaseEntity` 또는 `BaseTimeEntity` 상속
   - `FetchType.LAZY` 사용
   - Lombok `@Builder` 패턴

2. **Repository**
   - N+1 방지를 위한 Fetch Join 쿼리 작성
   - QueryDSL 활용

3. **Service**
   - Class-level `@Transactional(readOnly = true)`
   - Write 메서드만 `@Transactional`

4. **Controller**
   - `ApiResponse<T>` 사용
   - Swagger 어노테이션 작성

### N+1 문제 해결 예시

```java
@Query("SELECT DISTINCT m FROM Member m " +
       "LEFT JOIN FETCH m.memberFoodList mf " +
       "LEFT JOIN FETCH mf.food")
List<Member> findAllWithFoods();
```

---

## 📚 참고 문서

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 개발 가이드 (상세)
- [ERD.dbml](./docs/ERD.dbml) - 데이터베이스 ERD
- [DEPLOYMENT.md](./docs/DEPLOYMENT.md) - 배포 가이드
- [CICD_SETUP.md](./docs/CICD_SETUP.md) - GitHub Actions 설정

---

## 🏗️ 배포

이 프로젝트는 Mac Mini 자체 호스팅 환경에 배포됩니다:

- **CI/CD**: GitHub Actions
- **터널링**: Cloudflare Tunnel
- **모니터링**: Spring Boot Actuator

자세한 내용은 [DEPLOYMENT.md](./docs/DEPLOYMENT.md)를 참조하세요.

---

## 📝 License

이 프로젝트는 학습 목적의 프로젝트입니다.

---

## 👥 Contributors

UMC 9기 Spring Boot 스터디원들

---

## 🔗 Links

- [UMC Official](https://www.makeus.in/umc)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [JPA Best Practices](https://docs.spring.io/spring-data/jpa/reference/)

---

<div align="center">

**Made with ❤️ by UMC 9th**

</div>
