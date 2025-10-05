# Week3 API 명세서 📋

UMC 9th Spring Boot - 음식 배달 & 미션 앱 API 설계

---

## 📌 요구사항

### 필수 구현 기능
1. **홈 화면** - 마이 페이지 리뷰 작성, 미션 목록 조회
2. **미션 페이지** - 진행중/완료 미션 조회, 미션 성공 누르기
3. **회원 가입** - 소셜 로그인 고려 X

### 필수 활용 개념
- API Endpoint
- Request Body
- Request Header
- Query String
- Path Variable

---

## 🔐 공통 사항

### Base URL
```
https://api.umc-mission.com
```

### 인증 방식
모든 인증 필요 API는 Request Header에 JWT 토큰 포함

```http
Authorization: Bearer {access_token}
```

### 공통 응답 구조
```json
{
  "success": true | false,
  "message": "응답 메시지",
  "data": { /* 실제 데이터 */ },
  "error": { /* 에러 정보 (실패시) */ }
}
```

### HTTP 상태 코드
| 코드 | 의미 | 사용 예시 |
|------|------|----------|
| 200 | OK | 조회/수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 유효성 검증 실패 |
| 401 | Unauthorized | 인증 필요 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 500 | Server Error | 서버 오류 |

---

## 1️⃣ 회원 가입 API

### 1.1 회원가입

**Endpoint**
```http
POST /api/users/signup
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "name": "김UMC",
  "phone": "01012345678",
  "address": "서울시 강남구",
  "userType": "normal"
}
```

**Request Body 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | O | 이메일 (UNIQUE) |
| password | string | O | 비밀번호 (8자 이상) |
| name | string | O | 이름 |
| phone | string | X | 휴대폰 번호 (소셜 로그인 시 NULL 가능) |
| address | string | X | 주소 |
| userType | enum | O | 회원 타입 (normal, business) |

**Response (201 Created)**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 123,
    "email": "user@example.com",
    "name": "김UMC",
    "userType": "normal",
    "createdAt": "2025-10-05T21:30:00"
  }
}
```

**Error Response (400 Bad Request)**
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_EMAIL",
    "message": "이미 존재하는 이메일입니다.",
    "field": "email"
  }
}
```

---

## 2️⃣ 홈 화면 API

### 2.1 새로운 미션 목록 조회 (커서 기반 페이징)

**Endpoint**
```http
GET /api/missions/available
```

**Request Header**
```http
Authorization: Bearer {access_token}
```

**Query String**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| regionId | integer | O | 선택된 지역 ID |
| cursor | integer | X | 커서 (이전 페이지 마지막 mission ID) |
| size | integer | X | 페이지 크기 (기본값: 10) |

**Request 예시**
```http
GET /api/missions/available?regionId=1&cursor=50&size=10
Authorization: Bearer eyJhbGciOiJIUzI1...
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "missions": [
      {
        "id": 45,
        "storeName": "맛있는 치킨집",
        "description": "7일 연속 방문 미션",
        "points": 1000,
        "daysLeft": 5,
        "endDate": "2025-10-10T23:59:59"
      },
      {
        "id": 44,
        "storeName": "한식 전문점",
        "description": "리뷰 작성 미션",
        "points": 500,
        "daysLeft": 3,
        "endDate": "2025-10-08T23:59:59"
      }
    ],
    "pagination": {
      "nextCursor": 43,
      "hasNext": true,
      "size": 10
    }
  }
}
```

**참고 SQL**
```sql
-- week0/docs/sql/홈 화면 쿼리.sql 기반
SELECT
    m.id AS cursor_id,
    s.name AS store_name,
    m.description,
    m.reward_points AS points,
    DATEDIFF(m.end_date, NOW()) AS days_left,
    m.end_date
FROM MISSIONS m
INNER JOIN STORES s ON m.store_id = s.id
LEFT JOIN USER_MISSIONS um ON um.mission_id = m.id AND um.user_id = ?
WHERE s.region_id = ?
  AND m.status = 'active'
  AND m.end_date > NOW()
  AND um.id IS NULL
  AND m.id < ?
ORDER BY m.id DESC
LIMIT 10;
```

---

## 3️⃣ 미션 페이지 API

### 3.1 내가 진행중/완료한 미션 목록 조회

**Endpoint**
```http
GET /api/missions/my
```

**Request Header**
```http
Authorization: Bearer {access_token}
```

**Query String**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| status | string | X | 미션 상태 (in_progress, completed) |
| cursor | integer | X | 커서 (이전 페이지 마지막 user_mission ID) |
| size | integer | X | 페이지 크기 (기본값: 10) |

**Request 예시**
```http
GET /api/missions/my?status=in_progress&cursor=100&size=10
Authorization: Bearer eyJhbGciOiJIUzI1...
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "missions": [
      {
        "id": 95,
        "points": 1000,
        "status": "in_progress",
        "storeName": "맛있는 치킨집",
        "description": "7일 연속 방문 미션",
        "showReviewButton": false,
        "startedAt": "2025-10-01T10:00:00"
      },
      {
        "id": 94,
        "points": 500,
        "status": "completed",
        "storeName": "한식 전문점",
        "description": "리뷰 작성 미션",
        "showReviewButton": true,
        "completedAt": "2025-10-03T15:30:00"
      }
    ],
    "pagination": {
      "nextCursor": 93,
      "hasNext": true,
      "size": 10
    }
  }
}
```

**참고 SQL**
```sql
-- week0/docs/sql/내가 진행중, 진행 완료한 미션 모아서 보는 쿼리.sql 기반
SELECT
    um.id AS cursor_id,
    m.reward_points AS points,
    um.status,
    s.name AS store_name,
    m.description,
    r.id IS NULL AS show_review_button
FROM USER_MISSIONS um
INNER JOIN MISSIONS m ON um.mission_id = m.id
INNER JOIN STORES s ON m.store_id = s.id
LEFT JOIN REVIEWS r ON r.user_id = um.user_id AND r.store_id = m.store_id
WHERE um.user_id = ?
  AND um.status IN ('in_progress', 'completed')
  AND um.id < ?
ORDER BY um.id DESC
LIMIT 10;
```

---

### 3.2 미션 성공 누르기 (Control URI)

**Endpoint**
```http
POST /api/user-missions/{userMissionId}/complete
```

**Path Variable**
| 변수 | 타입 | 설명 |
|------|------|------|
| userMissionId | integer | 사용자 미션 ID |

**Request Header**
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "completionProof": "https://example.com/proof.jpg"
}
```

**Request Body 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| completionProof | string | X | 미션 완료 증빙 이미지 URL |

**Request 예시**
```http
POST /api/user-missions/95/complete
Authorization: Bearer eyJhbGciOiJIUzI1...
Content-Type: application/json

{
  "completionProof": "https://cdn.example.com/mission-proof-123.jpg"
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "message": "미션이 완료되었습니다!",
  "data": {
    "userMissionId": 95,
    "missionId": 45,
    "status": "completed",
    "earnedPoints": 1000,
    "completedAt": "2025-10-05T22:00:00",
    "totalPoints": 5000
  }
}
```

**Error Response (400 Bad Request)**
```json
{
  "success": false,
  "error": {
    "code": "ALREADY_COMPLETED",
    "message": "이미 완료된 미션입니다."
  }
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "error": {
    "code": "MISSION_NOT_FOUND",
    "message": "미션을 찾을 수 없습니다."
  }
}
```

**비즈니스 로직**
1. 미션 상태를 `completed`로 변경
2. 포인트 적립 (POINT_TRANSACTIONS 테이블에 INSERT)
3. 사용자 총 포인트 업데이트
4. 알림 발송 (NOTIFICATIONS 테이블에 INSERT)

---

## 4️⃣ 마이페이지 - 리뷰 작성 API

### 4.1 리뷰 작성

**Endpoint**
```http
POST /api/reviews
```

**Request Header**
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "orderId": 123,
  "storeId": 45,
  "rating": 4.5,
  "content": "음식이 정말 맛있었어요! 다음에도 주문할게요.",
  "images": [
    "https://cdn.example.com/review-image-1.jpg",
    "https://cdn.example.com/review-image-2.jpg"
  ],
  "isPublic": true
}
```

**Request Body 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| orderId | integer | O | 주문 ID (UNIQUE - 한 주문당 하나의 리뷰) |
| storeId | integer | O | 가게 ID |
| rating | decimal | O | 평점 (1.0 ~ 5.0) |
| content | string | X | 리뷰 내용 |
| images | array | X | 리뷰 이미지 URL 배열 (최대 5개) |
| isPublic | boolean | X | 공개 여부 (기본값: true) |

**Response (201 Created)**
```json
{
  "success": true,
  "message": "리뷰가 작성되었습니다.",
  "data": {
    "reviewId": 789,
    "orderId": 123,
    "storeId": 45,
    "rating": 4.5,
    "content": "음식이 정말 맛있었어요! 다음에도 주문할게요.",
    "images": [
      {
        "id": 1,
        "url": "https://cdn.example.com/review-image-1.jpg",
        "order": 1
      },
      {
        "id": 2,
        "url": "https://cdn.example.com/review-image-2.jpg",
        "order": 2
      }
    ],
    "isPublic": true,
    "createdAt": "2025-10-05T22:15:00"
  }
}
```

**Error Response (400 Bad Request)**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_RATING",
    "message": "평점은 1.0~5.0 사이여야 합니다.",
    "field": "rating",
    "rejectedValue": 6.0
  }
}
```

**Error Response (409 Conflict)**
```json
{
  "success": false,
  "error": {
    "code": "DUPLICATE_REVIEW",
    "message": "이미 해당 주문에 대한 리뷰가 존재합니다.",
    "field": "orderId"
  }
}
```

**참고 SQL**
```sql
-- week0/docs/sql/리뷰를 작성하는 쿼리.sql 기반

-- 리뷰 등록
INSERT INTO REVIEWS (
    user_id,
    store_id,
    order_id,
    rating,
    content,
    is_public,
    created_at,
    updated_at
) VALUES (?, ?, ?, ?, ?, true, NOW(), NOW());

-- 리뷰 이미지 등록 (다중 이미지)
INSERT INTO REVIEW_IMAGES (
    review_id,
    image_url,
    image_order,
    created_at
) VALUES (?, ?, ?, NOW());
```

**비즈니스 로직**
1. 주문 ID 중복 체크 (UNIQUE 제약)
2. 리뷰 테이블에 INSERT
3. 이미지 배열이 있으면 REVIEW_IMAGES 테이블에 순서대로 INSERT
4. 가게 평점 자동 계산 (트리거 or 배치)

---

### 4.2 마이페이지 정보 조회

**Endpoint**
```http
GET /api/users/me
```

**Request Header**
```http
Authorization: Bearer {access_token}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "name": "김UMC",
    "email": "user@example.com",
    "phone": "01012345678",
    "points": 5000,
    "address": "서울시 강남구",
    "userType": "normal",
    "createdAt": "2025-09-01T10:00:00"
  }
}
```

**참고 SQL**
```sql
-- week0/docs/sql/마이페이지 화면 쿼리.sql 기반
SELECT
    id,
    name,
    email,
    phone,
    points,
    address,
    user_type,
    created_at
FROM USERS
WHERE id = ?;
```

---

## 📊 API 엔드포인트 요약

| 기능 | Method | Endpoint | 인증 | 주요 개념 |
|------|--------|----------|------|----------|
| 회원가입 | POST | `/api/users/signup` | X | Request Body |
| 새 미션 목록 | GET | `/api/missions/available` | O | Query String, Header |
| 내 미션 목록 | GET | `/api/missions/my` | O | Query String, Header |
| 미션 완료 | POST | `/api/user-missions/{id}/complete` | O | Path Variable, Control URI |
| 리뷰 작성 | POST | `/api/reviews` | O | Request Body, Header |
| 마이페이지 | GET | `/api/users/me` | O | Header |

---

## 🎯 핵심 설계 포인트

### 1. 커서 기반 페이징 (Cursor-based Pagination)
- **장점**: 실시간 데이터 변경에 안정적
- **구현**: `cursor` 파라미터로 마지막 ID 전달
- **응답**: `nextCursor`와 `hasNext` 포함

### 2. Control URI 활용
- **미션 완료**: `POST /user-missions/{id}/complete`
- 복잡한 비즈니스 로직을 명확한 동사로 표현

### 3. 다중 이미지 처리
- 리뷰 이미지를 별도 테이블(REVIEW_IMAGES)로 정규화
- `image_order` 필드로 순서 관리

### 4. 자동 계산 필드
- 가게 평점: 리뷰 작성 시 트리거로 자동 업데이트
- 미션 참여자 수: USER_MISSIONS 변경 시 자동 동기화

### 5. 에러 처리
- 명확한 에러 코드와 메시지
- 실패한 필드와 값 포함

---

## 🚀 다음 단계

- [ ] Spring Boot Controller 구현
- [ ] Service Layer 비즈니스 로직 작성
- [ ] JPA Entity 및 Repository 설계
- [ ] 예외 처리 및 Validation
- [ ] API 테스트 코드 작성

**참고 문서**:
- `01_api_개념.md` - API 기초 개념
- `02_api_개념+코드구현.md` - Spring Boot 구현 가이드
- `../erd/02_week3_mission.md` - 데이터베이스 ERD
