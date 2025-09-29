# 음식 배달 & 미션 앱 ERD

UMC Spring Week0 미션 - 와이어프레임과 IA 분석을 바탕으로 설계한 완벽한 ERD

```mermaid
erDiagram
    %% 사용자 관리
    USERS {
        int id PK
        string email "UNIQUE, NOT NULL"
        string password "NOT NULL"
        string name "NOT NULL"
        string phone "UNIQUE when NOT NULL"
        string address
        enum user_type "normal,business"
        int points "DEFAULT 0"
        datetime created_at
        datetime updated_at
        boolean is_active "DEFAULT true"
    }

    %% 지역 관리
    REGIONS {
        int id PK
        string name "NOT NULL"
        string code "UNIQUE"
        decimal latitude
        decimal longitude
        datetime created_at
    }

    %% 가게 관리
    STORES {
        int id PK
        int owner_id FK "business user"
        int region_id FK
        string name "NOT NULL"
        string description
        string address "NOT NULL"
        decimal latitude
        decimal longitude
        string phone
        string business_hours
        decimal avg_rating "DEFAULT 0.0, AUTO-CALC"
        int total_reviews "DEFAULT 0, AUTO-CALC"
        enum status "active,inactive,suspended"
        datetime created_at
        datetime updated_at
    }

    %% 카테고리 관리
    CATEGORIES {
        int id PK
        string name "NOT NULL"
        string description
        string icon_url
        boolean is_active "DEFAULT true"
        datetime created_at
    }

    %% 가게-카테고리 연결
    STORE_CATEGORIES {
        int store_id FK
        int category_id FK
        datetime created_at
    }

    %% 메뉴 관리
    MENU_ITEMS {
        int id PK
        int store_id FK
        string name "NOT NULL"
        string description
        decimal price "NOT NULL"
        string image_url
        boolean is_available "DEFAULT true"
        boolean is_signature "DEFAULT false"
        datetime created_at
        datetime updated_at
    }

    %% 주문 관리
    ORDERS {
        int id PK
        int user_id FK
        int store_id FK
        string order_number "UNIQUE"
        decimal total_amount "NOT NULL"
        decimal delivery_fee "DEFAULT 0"
        string delivery_address "NOT NULL"
        string delivery_phone
        enum status "pending,confirmed,preparing,delivering,delivered,cancelled"
        string payment_method
        string special_requests
        datetime ordered_at
        datetime delivered_at
        datetime created_at
        datetime updated_at
    }

    %% 주문 상세
    ORDER_ITEMS {
        int id PK
        int order_id FK
        int menu_item_id FK
        int quantity "NOT NULL"
        decimal unit_price "NOT NULL"
        decimal total_price "NOT NULL"
        string special_requests
        datetime created_at
    }

    %% 리뷰 관리
    REVIEWS {
        int id PK
        int user_id FK
        int store_id FK
        int order_id FK "UNIQUE"
        decimal rating "NOT NULL, 1-5"
        string content
        string image_urls "JSON"
        boolean is_public "DEFAULT true"
        datetime created_at
        datetime updated_at
    }

    %% 미션 관리
    MISSIONS {
        int id PK
        int store_id FK
        string title "NOT NULL"
        string description "NOT NULL"
        int reward_points "NOT NULL"
        int max_participants
        int current_participants "DEFAULT 0, AUTO-CALC"
        datetime start_date
        datetime end_date
        enum status "active,paused,completed,cancelled"
        string mission_type "visit,order,review"
        json requirements "미션 완료 조건"
        datetime created_at
        datetime updated_at
    }

    %% 사용자 미션 참여
    USER_MISSIONS {
        int id PK
        int user_id FK
        int mission_id FK
        enum status "pending,in_progress,completed,failed,cancelled"
        datetime started_at
        datetime completed_at
        int earned_points "DEFAULT 0"
        json progress_data "진행 상황 데이터"
        datetime created_at
        datetime updated_at
    }

    %% 포인트 내역
    POINT_TRANSACTIONS {
        int id PK
        int user_id FK
        int points "positive=earn, negative=spend"
        string transaction_type "mission_reward,order_payment,refund"
        string description
        int related_id "mission_id or order_id"
        datetime created_at
    }

    %% 알림 관리
    NOTIFICATIONS {
        int id PK
        int user_id FK
        string title "NOT NULL"
        string content "NOT NULL"
        string type "mission,order,review,system"
        int related_id
        boolean is_read "DEFAULT false"
        datetime created_at
        datetime read_at
    }

    %% 관계 정의
    USERS ||--o{ STORES : ""
    USERS ||--o{ ORDERS : ""
    USERS ||--o{ REVIEWS : ""
    USERS ||--o{ USER_MISSIONS : ""
    USERS ||--o{ POINT_TRANSACTIONS : ""
    USERS ||--o{ NOTIFICATIONS : ""

    REGIONS ||--o{ STORES : ""

    STORES ||--o{ MENU_ITEMS : ""
    STORES ||--o{ ORDERS : ""
    STORES ||--o{ REVIEWS : ""
    STORES ||--o{ MISSIONS : ""
    STORES ||--o{ STORE_CATEGORIES : ""

    CATEGORIES ||--o{ STORE_CATEGORIES : ""

    ORDERS ||--o{ ORDER_ITEMS : ""
    ORDERS ||--|| REVIEWS : ""

    MENU_ITEMS ||--o{ ORDER_ITEMS : ""

    MISSIONS ||--o{ USER_MISSIONS : ""

    %% 복합키 정의
    %% STORE_CATEGORIES: PRIMARY KEY (store_id, category_id)
    %% USER_MISSIONS: UNIQUE (user_id, mission_id)
```

## 📋 주요 설계 포인트

### 1. 사용자 시스템 (IA 기반)

- **일반/기업 사용자** 구분 (user_type)
- **기본 사용자 정보** 관리 (이름, 이메일, 전화번호)
- **휴대폰 번호** 선택적 입력 가능 (소셜 로그인 지원)
- **포인트 시스템** 통합 관리
- **지역 기반** 서비스 제공

### 2. 가게 및 메뉴 관리 (와이어프레임 기반)

- **위치 기반** 검색 지원 (위도/경도)
- **카테고리별** 분류 시스템
- **평점 자동 계산** (avg_rating, total_reviews) - 트리거로 실시간 동기화
- **메뉴 시그니처** 표시 기능

### 3. 주문 시스템

- **주문 상태** 실시간 추적
- **배송비 별도** 관리
- **특별 요청사항** 지원
- **결제 방식** 다양화

### 4. 리뷰 시스템

- **주문 기반** 리뷰 (1:1 관계)
- **이미지 리뷰** 지원 (JSON 배열)
- **공개/비공개** 설정 가능

### 5. 미션 시스템 (핵심 기능 - IA 7일 미션)

- **가게별 미션** 생성 가능
- **참여자 수 제한** 기능 - 트리거로 실시간 동기화
- **다양한 미션 타입** (방문, 주문, 리뷰)
- **진행 상황** JSON으로 유연하게 관리
- **포인트 보상** 시스템

### 6. 알림 & 포인트 (마이페이지 대응)

- **통합 알림** 시스템
- **포인트 내역** 상세 추적
- **트랜잭션 타입** 명확한 구분

## 🎯 기획자 요구사항 완벽 반영

### ✅ IA 구조 대응

- **온보딩**: 로그인 → 회원가입 → 선호조사 (USERS 테이블)
- **홈**: 내가 받은/수행중/완료한 미션 (USER_MISSIONS 상태별 조회)
- **미션**: 새로운 미션, 리뷰 요청 (MISSIONS 테이블)
- **지도**: 가게 리스트, 가게 정보 (STORES + 위치 데이터)
- **마이페이지**: 포인트, 정보변경, 로그아웃 (USERS + POINT_TRANSACTIONS)

### ✅ 와이어프레임 화면 대응

- **미션 목록**: 7일 미션 표시 및 참여 (MISSIONS + USER_MISSIONS)
- **지도 검색**: 위치 기반 가게 검색 (STORES 위도/경도)
- **가게 상세**: 메뉴, 리뷰, 주문 (MENU_ITEMS + REVIEWS + ORDERS)
- **리뷰 작성**: 별점, 텍스트, 이미지 (REVIEWS 테이블)
- **포인트 관리**: 적립/사용 내역 (POINT_TRANSACTIONS)

### ✅ Week0 미션 수준에 적합

- **11개 핵심 테이블**로 모든 기능 구현 가능
- **과도한 최적화 없이** 기능 중심 설계
- **깔끔한 관계선**으로 가독성 확보

## 🔧 핵심 개선사항 (2024년 최신화)

### 자동 동기화 시스템
- **미션 참여자 수**: user_missions 테이블 변경 시 자동 업데이트
- **가게 평점**: reviews 테이블 변경 시 자동 계산 및 업데이트
- **데이터 일관성**: 트리거를 통한 실시간 동기화 보장

### 사용자 경험 개선
- **소셜 로그인 지원**: 휴대폰 번호 선택적 입력 (NULL 허용)
- **실시간 반영**: 미션 참여/리뷰 작성 즉시 화면 업데이트
- **정확한 정보**: 계산된 값들의 실시간 정확성 보장
