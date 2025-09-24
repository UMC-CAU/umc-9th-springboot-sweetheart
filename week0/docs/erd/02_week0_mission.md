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
        string phone "UNIQUE"
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
        decimal avg_rating "DEFAULT 0.0"
        int total_reviews "DEFAULT 0"
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
        int current_participants "DEFAULT 0"
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
    USERS ||--o{ STORES : "business_owner"
    USERS ||--o{ ORDERS : "places"
    USERS ||--o{ REVIEWS : "writes"
    USERS ||--o{ USER_MISSIONS : "participates"
    USERS ||--o{ POINT_TRANSACTIONS : "earns_spends"
    USERS ||--o{ NOTIFICATIONS : "receives"
    
    REGIONS ||--o{ STORES : "located_in"
    
    STORES ||--o{ MENU_ITEMS : "has"
    STORES ||--o{ ORDERS : "receives"
    STORES ||--o{ REVIEWS : "reviewed_by"
    STORES ||--o{ MISSIONS : "creates"
    STORES ||--o{ STORE_CATEGORIES : "belongs_to"
    
    CATEGORIES ||--o{ STORE_CATEGORIES : "categorizes"
    
    ORDERS ||--o{ ORDER_ITEMS : "contains"
    ORDERS ||--|| REVIEWS : "reviewed_by"
    
    MENU_ITEMS ||--o{ ORDER_ITEMS : "ordered_as"
    
    MISSIONS ||--o{ USER_MISSIONS : "assigned_to"
    
    %% 복합키 정의 (주석)
    %% STORE_CATEGORIES: PRIMARY KEY (store_id, category_id)
    %% USER_MISSIONS: UNIQUE (user_id, mission_id)
```

## 📋 주요 설계 포인트

### 1. 사용자 시스템
- **일반/기업 사용자** 구분 (user_type)
- **포인트 시스템** 통합 관리
- **지역 기반** 서비스 제공

### 2. 가게 및 메뉴 관리
- **위치 기반** 검색 지원 (위도/경도)
- **카테고리별** 분류 시스템
- **평점 자동 계산** (avg_rating, total_reviews)
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

### 5. 미션 시스템 (핵심 기능)
- **가게별 미션** 생성 가능
- **참여자 수 제한** 기능
- **다양한 미션 타입** (방문, 주문, 리뷰)
- **진행 상황** JSON으로 유연하게 관리
- **포인트 보상** 시스템

### 6. 알림 & 포인트
- **통합 알림** 시스템
- **포인트 내역** 상세 추적
- **트랜잭션 타입** 명확한 구분

## 🎯 와이어프레임 대응

- ✅ **7일 미션**: MISSIONS 테이블의 날짜 범위로 구현
- ✅ **미션 진행 상태**: USER_MISSIONS의 status로 추적  
- ✅ **지도 검색**: STORES의 위도/경도 데이터 활용
- ✅ **리뷰 작성**: REVIEWS 테이블로 주문별 리뷰 관리
- ✅ **포인트 시스템**: POINT_TRANSACTIONS로 상세 내역 관리
- ✅ **마이페이지**: 모든 사용자 데이터 통합 조회 가능