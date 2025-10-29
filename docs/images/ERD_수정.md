# ERD 수정사항

**작성일:** 2025-10-29
**목적:** Chapter5 화면 구현을 위한 엔티티 필드 추가

---

## 📋 수정 개요

현재 ERD로 구현 가능: 60% → 필수 수정 후: **100% 가능**

---

## ✅ 필수 수정사항 (3가지)

### 1. Mission 엔티티 - name 필드 추가 ⭐

**문제:** 홈 화면에서 미션 이름이 필요한데 현재는 conditional, point, deadline만 있음

**수정:**

```java
@Column(name = "name", nullable = false)
private String name;  // "반아향생미라탕 10,000원 이상 식사"
```

---

### 2. Store 엔티티 - 카테고리 추가 ⭐

**문제:** "중식당", "한식당" 등 카테고리 표시 필요

**Food FK 재활용**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "food_id", nullable = false)
private Food food;  // KOREAN, CHINESE, JAPANESE 등
```

---

### 3. MemberMission - status enum으로 변경 ⭐

**문제:** Boolean isComplete는 true/false 2가지만 가능, 실제로는 3가지 상태 필요

**수정:**

```java
// isComplete 삭제하고
@Column(name = "status", nullable = false)
@Enumerated(EnumType.STRING)
@Builder.Default
private MissionStatus status = MissionStatus.AVAILABLE;
```

**MissionStatus enum 생성:**

```java
public enum MissionStatus {
    AVAILABLE,    // 진행가능 (시작 전)
    IN_PROGRESS,  // 진행중 (도전 버튼 누름)
    COMPLETED     // 진행완료 (리뷰 작성)
}
```

---

## 🔧 선택 사항

### 4. Member 엔티티 - 휴대폰 인증

**이미 완료:** address는 String으로 변경됨 ✅

**현재 방법:** `phoneNumber == null`이면 미인증

---

### 5. ReviewPhoto - imageOrder 필드

이미지 순서 보장을 위한 Integer 필드

```java
@Column(name = "image_order", nullable = false)
private Integer imageOrder;
```

---
