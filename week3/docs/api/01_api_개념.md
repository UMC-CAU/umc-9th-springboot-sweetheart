# API 기초 개념 정리 📚

REST API 명세서 작성을 위한 핵심 개념 정리

---

## 1️⃣ API Endpoint (API 엔드포인트)

### 🤔 뭐지?
**서버의 특정 기능에 접근하기 위한 URL 주소**

### 구조
```
[HTTP 메서드] [Base URL]/[경로]
```

### 주요 HTTP 메서드
| 메서드 | 용도 | 예시 |
|--------|------|------|
| **GET** | 데이터 조회 (읽기) | `GET /api/missions` |
| **POST** | 데이터 생성 (쓰기) | `POST /api/reviews` |
| **PATCH** | 데이터 일부 수정 | `PATCH /api/users/123` |
| **PUT** | 데이터 전체 교체 | `PUT /api/users/123` |
| **DELETE** | 데이터 삭제 | `DELETE /api/reviews/123` |

---

## 2️⃣ Request Body (요청 바디)

### 🤔 뭐지?
**클라이언트가 서버로 데이터를 전송할 때 HTTP 본문에 담는 데이터 (JSON)**

### 특징
- POST, PATCH, PUT에서 사용
- GET, DELETE는 일반적으로 사용 안 함

### 예시
```json
POST /api/users/signup
{
  "email": "user@example.com",
  "password": "password123",
  "name": "김UMC"
}
```

---

## 3️⃣ Request Header (요청 헤더)

### 🤔 뭐지?
**HTTP 요청의 메타데이터 (인증 토큰, 데이터 형식 등)**

### 주요 헤더
| 헤더 | 용도 | 예시 |
|------|------|------|
| Content-Type | 데이터 형식 | `application/json` |
| Authorization | 인증 토큰 | `Bearer {token}` |
| Accept | 받고 싶은 형식 | `application/json` |

### 예시
```http
GET /api/missions/my
Authorization: Bearer eyJhbGciOiJIUzI1...
```

---

## 4️⃣ Query String (쿼리 스트링)

### 🤔 뭐지?
**URL 뒤에 `?`로 전달하는 파라미터**

### 용도
검색, 필터링, 정렬, 페이징

### 예시
```http
GET /api/missions?status=active&page=1&size=10
```
- `status=active` - 진행 중인 미션만
- `page=1` - 1페이지
- `size=10` - 10개씩

---

## 5️⃣ Path Variable (경로 변수)

### 🤔 뭐지?
**URL 경로 안에 포함된 변수 (특정 리소스 식별)**

### 예시
```http
GET /api/missions/{missionId}
GET /api/missions/123  ← 123이 Path Variable
```

---

## 6️⃣ Response Body (응답 바디)

### 🤔 뭐지?
**서버가 클라이언트에게 보내는 데이터 (JSON)**

### 표준 응답 구조 (권장)
```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { /* 실제 데이터 */ },
  "error": { /* 에러 정보 (실패시) */ }
}
```

### 성공 예시
```json
{
  "success": true,
  "data": {
    "missions": [...],
    "pagination": {
      "currentPage": 1,
      "totalPages": 5
    }
  }
}
```

### 실패 예시
```json
{
  "success": false,
  "error": {
    "code": "INVALID_RATING",
    "message": "평점은 1~5 사이여야 합니다"
  }
}
```

---

## 7️⃣ Soft Delete (소프트 삭제)

### 🤔 뭐지?
**실제 삭제하지 않고 삭제 플래그만 표시 (`is_deleted = true`)**

### ⚠️ 중요
**URL 설계에는 영향 없음!**
- API: `DELETE /api/reviews/123` (동일)
- DB: `UPDATE reviews SET is_deleted = true` (내부 처리)

### Week3 사용 예시
- 리뷰 삭제
- 미션 취소
- 회원 탈퇴

---

## 8️⃣ Control URI (컨트롤 URI)

### 🤔 뭐지?
**REST 원칙 예외로 동작(동사)을 URL에 포함**

### 언제 사용?
복잡한 비즈니스 로직 (여러 리소스 변경, 프로세스 실행)

### Week3 사용 예시
| 기능 | REST (❌) | Control URI (✅) |
|------|----------|-----------------|
| 미션 완료 | `PATCH /user-missions/123` | `POST /user-missions/123/complete` |
| 주문 취소 | `DELETE /orders/123` | `POST /orders/123/cancel` |
| 리뷰 신고 | `POST /review-reports` | `POST /reviews/123/report` |

---

## 📊 전체 비교표

| 구분 | 위치/방식 | 용도 | 예시 |
|------|----------|------|------|
| **API Endpoint** | URL 전체 | 기능 접근 주소 | `GET /api/users` |
| **Path Variable** | URL 경로 안 | 리소스 식별 | `/users/{id}` |
| **Query String** | URL 뒤 `?` | 검색/필터링/정렬 | `?status=active&page=1` |
| **Request Body** | HTTP 요청 본문 | 데이터 전송 | `{"name": "김UMC"}` |
| **Response Body** | HTTP 응답 본문 | 데이터 응답 | `{"success": true, ...}` |
| **Request Header** | HTTP 요청 헤더 | 인증/메타데이터 | `Authorization: Bearer ...` |
| **Soft Delete** | DB 구현 방식 | 복구 가능 삭제 | `is_deleted = true` |
| **Control URI** | URL 동사 사용 | 복잡한 비즈니스 로직 | `/orders/123/cancel` |

---

## 💡 꿀팁

### REST API 설계 원칙
1. **명사 사용**: `/getUsers` ❌ → `/users` ✅
2. **복수형 사용**: `/user` ❌ → `/users` ✅
3. **계층 구조**: `/stores/123/reviews` ✅
4. **소문자 + 하이픈**: `/user-profiles` ✅

### HTTP 상태 코드
| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 201 | 생성됨 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 404 | 찾을 수 없음 |
| 500 | 서버 오류 |

### Query String vs Request Body
- **Query String**: 조회 (검색, 필터링, 정렬, 페이징)
- **Request Body**: 변경 (생성, 수정)

---

**다음 문서**: `02_spring_boot_implementation.md` (구현 예제)
