# API 기초 개념 정리 📚

Spring Boot REST API를 설계하기 위해 꼭 알아야 할 핵심 개념들을 초보자 관점에서 정리했습니다.

---

## 1️⃣ API Endpoint (API 엔드포인트)

### 🤔 뭐지?
**서버의 특정 기능에 접근하기 위한 URL 주소**입니다.
쉽게 말해, "이 주소로 요청하면 이 기능을 실행해줄게!"라고 약속된 URL입니다.

### 📝 구성 요소
```
[HTTP 메서드] [Base URL]/[경로]
```

### 💡 실제 예시
```http
# 사용자 목록 조회
GET https://api.example.com/users

# 특정 사용자 조회
GET https://api.example.com/users/123

# 새 사용자 생성
POST https://api.example.com/users

# 사용자 정보 수정
PATCH https://api.example.com/users/123

# 사용자 삭제
DELETE https://api.example.com/users/123
```

### 🎯 주요 HTTP 메서드
| 메서드 | 용도 | 예시 |
|--------|------|------|
| **GET** | 데이터 조회 (읽기) | 미션 목록 가져오기 |
| **POST** | 데이터 생성 (쓰기) | 새 리뷰 작성하기 |
| **PATCH** | 데이터 일부 수정 | 회원 정보 수정하기 |
| **PUT** | 데이터 전체 교체 | 프로필 전체 업데이트 |
| **DELETE** | 데이터 삭제 | 리뷰 삭제하기 |

---

## 2️⃣ Request Body (요청 바디)

### 🤔 뭐지?
**클라이언트가 서버로 데이터를 전송할 때, HTTP 요청의 본문에 담아 보내는 데이터**입니다.
주로 **POST, PATCH, PUT** 요청에서 사용합니다.

### 📝 특징
- JSON 형식으로 많이 사용 (Spring Boot 기본)
- GET, DELETE 요청에는 일반적으로 사용 안 함
- 복잡한 데이터 구조 전송 가능

### 💡 실제 예시

#### 회원가입 요청
```http
POST /api/users/signup
Content-Type: application/json

{
  "email": "sweetheart@umc.com",
  "password": "securePassword123!",
  "name": "김UMC",
  "phone": "01012345678",
  "address": "서울시 강남구"
}
```

#### 리뷰 작성 요청
```http
POST /api/reviews
Content-Type: application/json

{
  "orderId": 123,
  "storeId": 45,
  "rating": 4.5,
  "content": "음식이 정말 맛있었어요!",
  "images": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.jpg"
  ]
}
```

### ⚙️ Spring Boot 코드 예시
```java
@PostMapping("/api/reviews")
public ResponseEntity<Review> createReview(@RequestBody ReviewRequest request) {
    // request.getOrderId(), request.getRating() 등으로 접근
    Review review = reviewService.create(request);
    return ResponseEntity.ok(review);
}
```

---

## 3️⃣ Request Header (요청 헤더)

### 🤔 뭐지?
**HTTP 요청에 대한 메타데이터(부가 정보)를 담는 부분**입니다.
인증 토큰, 데이터 형식, 클라이언트 정보 등을 전달합니다.

### 📝 주요 헤더 종류

| 헤더 이름 | 용도 | 예시 값 |
|-----------|------|---------|
| **Content-Type** | 보내는 데이터 형식 | `application/json` |
| **Authorization** | 인증 토큰 | `Bearer eyJhbGciOiJIUzI1...` |
| **Accept** | 받고 싶은 데이터 형식 | `application/json` |
| **User-Agent** | 클라이언트 정보 | `Mozilla/5.0...` |

### 💡 실제 예시

#### 로그인 후 API 요청 (인증 필요)
```http
GET /api/missions/my
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

#### 파일 업로드 요청
```http
POST /api/reviews/images
Content-Type: multipart/form-data
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### ⚙️ Spring Boot 코드 예시
```java
@GetMapping("/api/missions/my")
public ResponseEntity<List<Mission>> getMyMissions(
    @RequestHeader("Authorization") String authToken
) {
    // authToken에서 "Bearer " 제거 후 검증
    String token = authToken.replace("Bearer ", "");
    List<Mission> missions = missionService.findByUser(token);
    return ResponseEntity.ok(missions);
}
```

---

## 4️⃣ Query String (쿼리 스트링)

### 🤔 뭐지?
**URL 뒤에 `?`를 붙여 전달하는 파라미터**입니다.
주로 **검색, 필터링, 정렬, 페이징**에 사용합니다.

### 📝 구조
```
URL?key1=value1&key2=value2&key3=value3
```

### 💡 실제 예시

#### 미션 목록 조회 (필터링 + 페이징)
```http
GET /api/missions?status=active&page=1&size=10&sort=createdAt,desc
```
- `status=active`: 진행 중인 미션만
- `page=1`: 1페이지
- `size=10`: 한 페이지에 10개
- `sort=createdAt,desc`: 생성일 기준 내림차순

#### 가게 검색 (지역 + 카테고리)
```http
GET /api/stores?region=강남구&category=한식&minRating=4.0
```
- `region=강남구`: 강남구 지역
- `category=한식`: 한식 카테고리
- `minRating=4.0`: 평점 4.0 이상

### ⚙️ Spring Boot 코드 예시
```java
@GetMapping("/api/missions")
public ResponseEntity<Page<Mission>> getMissions(
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt,desc") String sort
) {
    // status, page, size, sort 값으로 데이터 조회
    Page<Mission> missions = missionService.findAll(status, page, size, sort);
    return ResponseEntity.ok(missions);
}
```

---

## 5️⃣ Path Variable (경로 변수)

### 🤔 뭐지?
**URL 경로 자체에 포함된 변수 값**입니다.
주로 **특정 리소스를 식별**할 때 사용합니다.

### 📝 구조
```
/api/리소스/{변수명}
```

### 💡 실제 예시

#### 특정 미션 상세 조회
```http
GET /api/missions/123
```
- `123`이 Path Variable (mission ID)

#### 특정 가게의 리뷰 목록 조회
```http
GET /api/stores/45/reviews
```
- `45`가 Path Variable (store ID)

#### 미션 상태 변경
```http
PATCH /api/missions/123/status
```
- `123`이 Path Variable (mission ID)

### ⚙️ Spring Boot 코드 예시
```java
@GetMapping("/api/missions/{missionId}")
public ResponseEntity<Mission> getMission(
    @PathVariable Long missionId
) {
    // missionId 값으로 특정 미션 조회
    Mission mission = missionService.findById(missionId);
    return ResponseEntity.ok(mission);
}

@GetMapping("/api/stores/{storeId}/reviews")
public ResponseEntity<List<Review>> getStoreReviews(
    @PathVariable Long storeId
) {
    // storeId에 해당하는 가게의 리뷰 조회
    List<Review> reviews = reviewService.findByStoreId(storeId);
    return ResponseEntity.ok(reviews);
}
```

---

## 6️⃣ Response Body (응답 바디)

### 🤔 뭐지?
**서버가 클라이언트에게 데이터를 응답할 때, HTTP 응답의 본문에 담아 보내는 데이터**입니다.
Request Body가 "보내는 데이터"라면, Response Body는 **"받는 데이터"**입니다!

### 📝 특징
- 거의 모든 API 응답에 포함됨 (GET, POST, PATCH 등)
- JSON 형식이 표준 (Spring Boot 자동 변환)
- 성공/실패 정보, 실제 데이터, 에러 메시지 등 포함

### 💡 실제 예시

#### 회원가입 성공 응답
```http
POST /api/users/signup
→ Response (201 Created)

{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 123,
    "email": "sweetheart@umc.com",
    "name": "김UMC",
    "createdAt": "2025-10-05T14:30:00"
  }
}
```

#### 미션 목록 조회 응답
```http
GET /api/missions?status=active
→ Response (200 OK)

{
  "success": true,
  "data": {
    "missions": [
      {
        "id": 1,
        "title": "7일 연속 방문 미션",
        "rewardPoints": 1000,
        "currentParticipants": 45,
        "maxParticipants": 100
      },
      {
        "id": 2,
        "title": "리뷰 작성 미션",
        "rewardPoints": 500,
        "currentParticipants": 12,
        "maxParticipants": 50
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 5,
      "totalItems": 48
    }
  }
}
```

#### 에러 응답 (실패 케이스)
```http
POST /api/reviews
→ Response (400 Bad Request)

{
  "success": false,
  "error": {
    "code": "INVALID_RATING",
    "message": "평점은 1~5 사이의 값이어야 합니다.",
    "field": "rating",
    "rejectedValue": 6
  }
}
```

### ⚙️ Spring Boot 코드 예시
```java
// 성공 응답 (단일 데이터)
@PostMapping("/api/users/signup")
public ResponseEntity<ApiResponse<UserResponse>> signup(@RequestBody SignupRequest request) {
    UserResponse user = userService.signup(request);

    ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
        .success(true)
        .message("회원가입이 완료되었습니다.")
        .data(user)
        .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// 성공 응답 (리스트 + 페이징)
@GetMapping("/api/missions")
public ResponseEntity<ApiResponse<MissionListResponse>> getMissions(
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "0") int page
) {
    Page<Mission> missions = missionService.findAll(status, page);

    MissionListResponse data = MissionListResponse.builder()
        .missions(missions.getContent())
        .pagination(PaginationInfo.from(missions))
        .build();

    return ResponseEntity.ok(ApiResponse.success(data));
}

// 에러 응답
@ExceptionHandler(InvalidRatingException.class)
public ResponseEntity<ApiResponse<Void>> handleInvalidRating(InvalidRatingException e) {
    ErrorResponse error = ErrorResponse.builder()
        .code("INVALID_RATING")
        .message(e.getMessage())
        .field("rating")
        .rejectedValue(e.getRejectedValue())
        .build();

    ApiResponse<Void> response = ApiResponse.<Void>builder()
        .success(false)
        .error(error)
        .build();

    return ResponseEntity.badRequest().body(response);
}
```

### 🎨 표준 응답 구조 설계 (권장)
```java
// 공통 응답 wrapper
public class ApiResponse<T> {
    private boolean success;      // 성공 여부
    private String message;       // 응답 메시지
    private T data;              // 실제 데이터
    private ErrorResponse error;  // 에러 정보 (실패시)
    private LocalDateTime timestamp; // 응답 시간
}

// 페이징 정보
public class PaginationInfo {
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private int pageSize;
}
```

---

## 7️⃣ Soft Delete (소프트 삭제)

### 🤔 뭐지?
**데이터를 실제로 삭제하지 않고, 삭제 플래그만 표시하는 방식**입니다.
DB에서 완전히 지우는 것(Hard Delete)과 달리, "삭제된 것처럼 보이게" 만듭니다.

### 📝 왜 사용할까?
- ✅ **데이터 복구 가능** - 실수로 삭제해도 복구 가능
- ✅ **감사 추적** - 누가 언제 삭제했는지 기록 유지
- ✅ **법적 요구사항** - 일정 기간 데이터 보관 의무
- ✅ **통계/분석** - 삭제된 데이터도 분석에 활용
- ❌ **저장공간 증가** - 실제로 삭제 안 되므로 용량 차지

### 💡 구현 방식

#### DB 테이블 설계
```sql
-- Soft Delete 적용 테이블 예시
CREATE TABLE reviews (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    rating DECIMAL(2,1),
    content TEXT,
    created_at DATETIME,
    updated_at DATETIME,

    -- Soft Delete 필드들
    is_deleted BOOLEAN DEFAULT FALSE,  -- 삭제 여부
    deleted_at DATETIME,               -- 삭제 시간
    deleted_by BIGINT                  -- 삭제한 사용자 ID
);
```

#### Spring Boot Entity
```java
@Entity
@Table(name = "reviews")
@SQLDelete(sql = "UPDATE reviews SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")  // 기본 조회시 삭제 제외
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long storeId;
    private BigDecimal rating;
    private String content;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
```

### 🔗 URL 설계와의 관계

**Soft Delete는 URL 설계에 직접 영향 없음!**
- DELETE 메서드는 동일하게 사용
- 내부적으로만 soft delete 처리
- 필요시 복구 API를 별도로 만들 수 있음

### 💡 실제 예시

#### 일반 삭제 (Hard Delete 처럼 보임)
```http
DELETE /api/reviews/123
→ Response (204 No Content)

{
  "success": true,
  "message": "리뷰가 삭제되었습니다."
}

# 실제로는 DB에서:
# UPDATE reviews SET is_deleted = true, deleted_at = NOW() WHERE id = 123
```

#### 삭제된 데이터 복구 API (선택사항)
```http
POST /api/reviews/123/restore
→ Response (200 OK)

{
  "success": true,
  "message": "리뷰가 복구되었습니다.",
  "data": {
    "id": 123,
    "content": "복구된 리뷰 내용",
    "restoredAt": "2025-10-05T15:00:00"
  }
}
```

### ⚙️ Spring Boot 구현 예시
```java
@DeleteMapping("/api/reviews/{reviewId}")
public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
    // JPA의 delete()를 호출하면 @SQLDelete 어노테이션의 쿼리 실행
    reviewService.delete(reviewId);

    return ResponseEntity.noContent().build();
}

// 복구 API (선택사항)
@PostMapping("/api/reviews/{reviewId}/restore")
public ResponseEntity<ApiResponse<Review>> restoreReview(@PathVariable Long reviewId) {
    Review restored = reviewService.restore(reviewId);

    return ResponseEntity.ok(ApiResponse.success(restored, "리뷰가 복구되었습니다."));
}

// Service 레이어
public void delete(Long reviewId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException());

    // @SQLDelete로 soft delete 실행
    reviewRepository.delete(review);
}

public Review restore(Long reviewId) {
    // 삭제된 것도 조회하려면 native query 필요
    Review review = reviewRepository.findByIdIncludingDeleted(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException());

    if (!review.getIsDeleted()) {
        throw new ReviewNotDeletedException();
    }

    review.setIsDeleted(false);
    review.setDeletedAt(null);
    return reviewRepository.save(review);
}
```

### 📌 Week3 미션에서 사용 예시
- **리뷰 삭제**: 사용자가 실수로 삭제해도 복구 가능
- **미션 취소**: 취소한 미션 기록은 남기되, 목록에서 제외
- **회원 탈퇴**: 법적 보관 기간 동안 데이터 유지

---

## 8️⃣ Control URI (제어 URI / 컨트롤 URI)

### 🤔 뭐지?
**REST 원칙에서 벗어나 "동작(동사)"을 URL에 포함하는 방식**입니다.
보통 REST는 명사만 사용하지만, 특정 동작은 동사를 써야 더 명확할 때 사용합니다.

### 📝 REST vs Control URI

| 구분 | REST 원칙 | Control URI |
|------|-----------|-------------|
| **기본 규칙** | 명사만 사용 | 동사 허용 |
| **예시** | `POST /orders` | `POST /orders/123/cancel` |
| **언제** | CRUD 작업 | 복잡한 비즈니스 로직 |

### 💡 왜 필요할까?

#### ❌ REST만으로는 표현이 애매한 경우
```http
# 주문 취소를 어떻게 표현할까?
PATCH /orders/123 { "status": "cancelled" }  # 애매함
DELETE /orders/123                            # 삭제처럼 보임

# ✅ Control URI로 명확하게!
POST /orders/123/cancel                       # 명확함!
```

#### ❌ 여러 리소스가 연관된 복잡한 동작
```http
# 미션 성공 처리 (미션 상태 변경 + 포인트 적립 + 알림 발송)
PATCH /user-missions/123 { ... }  # 너무 많은 일을 함축

# ✅ Control URI로 의도 명확화!
POST /user-missions/123/complete  # "완료 처리"라는 의도 명확
```

### 🎯 Control URI 사용 시기

| 상황 | REST 방식 | Control URI (권장) |
|------|-----------|-------------------|
| 단순 상태 변경 | `PATCH /users/123` | 필요없음 |
| 복잡한 비즈니스 동작 | 애매함 | `POST /orders/123/cancel` |
| 여러 리소스 변경 | 부적합 | `POST /missions/123/complete` |
| 프로세스 실행 | 표현 어려움 | `POST /payments/123/refund` |

### 💡 실제 예시 (Week3 미션)

#### 1. 미션 성공 누르기 (Control URI 적합)
```http
# ❌ REST 방식 (애매함)
PATCH /user-missions/123
{
  "status": "completed"  # 단순 상태 변경처럼 보임
}

# ✅ Control URI (명확함)
POST /api/user-missions/123/complete
{
  "completionProof": "https://example.com/proof.jpg"
}

→ Response (200 OK)
{
  "success": true,
  "message": "미션이 완료되었습니다!",
  "data": {
    "missionId": 123,
    "earnedPoints": 1000,
    "completedAt": "2025-10-05T14:30:00"
  }
}
```

#### 2. 주문 취소 (Control URI 적합)
```http
POST /api/orders/456/cancel
{
  "reason": "다른 메뉴로 변경하고 싶어요",
  "refundMethod": "point"
}

→ Response (200 OK)
{
  "success": true,
  "message": "주문이 취소되었습니다.",
  "data": {
    "orderId": 456,
    "refundAmount": 15000,
    "refundedAt": "2025-10-05T14:35:00"
  }
}
```

#### 3. 리뷰 신고 (Control URI 적합)
```http
POST /api/reviews/789/report
{
  "reason": "inappropriate_content",
  "description": "욕설이 포함되어 있습니다."
}
```

#### 4. 포인트 사용 (Control URI 적합)
```http
POST /api/users/me/points/use
{
  "amount": 5000,
  "orderId": 123
}
```

### ⚙️ Spring Boot 구현 예시
```java
// Control URI - 미션 완료 처리
@PostMapping("/api/user-missions/{userMissionId}/complete")
public ResponseEntity<ApiResponse<MissionCompleteResponse>> completeMission(
    @PathVariable Long userMissionId,
    @RequestBody MissionCompleteRequest request,
    @RequestHeader("Authorization") String token
) {
    // 복잡한 비즈니스 로직:
    // 1. 미션 상태 -> completed 변경
    // 2. 포인트 적립
    // 3. 알림 발송
    // 4. 통계 업데이트
    MissionCompleteResponse result = missionService.complete(userMissionId, request);

    return ResponseEntity.ok(ApiResponse.success(result, "미션이 완료되었습니다!"));
}

// Control URI - 주문 취소
@PostMapping("/api/orders/{orderId}/cancel")
public ResponseEntity<ApiResponse<OrderCancelResponse>> cancelOrder(
    @PathVariable Long orderId,
    @RequestBody OrderCancelRequest request
) {
    OrderCancelResponse result = orderService.cancel(orderId, request);
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

### 📌 Control URI 네이밍 규칙
1. **동사 사용 OK**: `/cancel`, `/complete`, `/approve`, `/reject`
2. **복합 동사도 OK**: `/send-notification`, `/mark-as-read`
3. **명확성 우선**: 길어도 의미가 명확한 게 중요
4. **일관성 유지**: 팀 내에서 동일한 패턴 사용

### 🎯 Week3 미션에서 Control URI 사용 예시

| 기능 | REST (부적합) | Control URI (적합) |
|------|---------------|-------------------|
| 미션 성공 누르기 | `PATCH /user-missions/123` | `POST /user-missions/123/complete` ✅ |
| 주문 취소 | `DELETE /orders/123` | `POST /orders/123/cancel` ✅ |
| 리뷰 신고 | `POST /review-reports` | `POST /reviews/123/report` ✅ |
| 포인트 사용 | `POST /point-transactions` | `POST /users/me/points/use` ✅ |
| 알림 읽음 처리 | `PATCH /notifications/123` | `POST /notifications/123/mark-as-read` ✅ |

---

## 📊 전체 비교 정리표

| 구분 | 위치/방식 | 용도 | 예시 |
|------|----------|------|------|
| **API Endpoint** | URL 전체 | 기능 접근 주소 | `GET /api/users` |
| **Path Variable** | URL 경로 안 | 리소스 식별 | `/users/123` |
| **Query String** | URL 뒤 `?` | 검색/필터링/정렬 | `/users?age=20&city=서울` |
| **Request Body** | HTTP 요청 본문 | 데이터 전송 (보내기) | `{"name": "김UMC"}` |
| **Response Body** | HTTP 응답 본문 | 데이터 응답 (받기) | `{"success": true, "data": {...}}` |
| **Request Header** | HTTP 요청 헤더 | 인증/메타데이터 | `Authorization: Bearer ...` |
| **Soft Delete** | DB 구현 방식 | 복구 가능한 삭제 | `is_deleted = true` (URL 무관) |
| **Control URI** | URL 동사 사용 | 복잡한 비즈니스 로직 | `POST /orders/123/cancel` |

---

## 🎯 언제 뭘 쓸까? (실전 가이드)

### 📌 데이터 조회할 때
```http
# ✅ 전체 목록 조회 (필터링/정렬)
GET /api/missions?status=active&sort=createdAt,desc

# ✅ 특정 항목 조회
GET /api/missions/123
```

### 📌 데이터 생성할 때
```http
# ✅ Request Body 사용
POST /api/missions
{
  "title": "7일 미션",
  "description": "매일 방문하기",
  "rewardPoints": 1000
}
```

### 📌 데이터 수정할 때
```http
# ✅ Path Variable + Request Body
PATCH /api/missions/123
{
  "status": "completed"
}
```

### 📌 인증이 필요한 요청
```http
# ✅ Request Header에 토큰
GET /api/missions/my
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 💡 꿀팁 모음

### ✅ REST API 설계 원칙
1. **명사 사용**: `/getUsers` ❌ → `/users` ✅
2. **복수형 사용**: `/user` ❌ → `/users` ✅
3. **계층 구조**: `/stores/123/reviews` ✅
4. **소문자 + 하이픈**: `/userProfiles` ❌ → `/user-profiles` ✅

### ✅ HTTP 상태 코드
| 코드 | 의미 | 예시 |
|------|------|------|
| **200** | 성공 | 조회/수정 성공 |
| **201** | 생성됨 | POST 성공 |
| **400** | 잘못된 요청 | 유효성 검증 실패 |
| **401** | 인증 필요 | 로그인 필요 |
| **404** | 찾을 수 없음 | 리소스 없음 |
| **500** | 서버 오류 | 서버 내부 에러 |

### ✅ Request Body vs Query String 선택 기준
- **Query String**: 검색, 필터링, 정렬, 페이징 (데이터 조회)
- **Request Body**: 생성, 수정 (데이터 변경)

---

## 🚀 다음 단계

이제 기본 개념을 이해했다면, Week3 미션의 실제 API를 설계해봅시다!

- [ ] 홈 화면 API 설계
- [ ] 미션 페이지 API 설계
- [ ] 회원가입 API 설계
- [ ] 마이페이지 리뷰 작성 API 설계

**다음 문서**: `02_week3_api_design.md`
