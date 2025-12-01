# UMC 9기 미션 분석 보고서

## 📚 목차
1. [@DynamicInsert/@DynamicUpdate 분석](#1-dynamicinsertdynamicupdate-분석)
2. [Rest Docs vs Swagger 비교 분석](#2-rest-docs-vs-swagger-비교-분석)
3. [실전 적용 가이드](#3-실전-적용-가이드)

---

## 1. @DynamicInsert/@DynamicUpdate 분석

### 1️⃣ 기본 JPA 쿼리 동작 원리

#### 기본 동작 (Static SQL)
JPA는 기본적으로 **정적 SQL**을 사용합니다. 엔티티가 로드될 때 모든 필드에 대한 SQL을 미리 생성하여 캐싱합니다.

```java
@Entity
public class Member {
    @Id
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
}
```

**INSERT 시 생성되는 SQL (모든 컬럼 포함):**
```sql
INSERT INTO member (id, name, email, phone, address)
VALUES (?, ?, ?, ?, ?)
-- null 값도 모두 포함하여 insert
```

**UPDATE 시 생성되는 SQL (모든 컬럼 포함):**
```sql
UPDATE member
SET name = ?, email = ?, phone = ?, address = ?
WHERE id = ?
-- 변경되지 않은 필드도 모두 update
```

### 2️⃣ @DynamicInsert/@DynamicUpdate 적용 시 동작

#### @DynamicInsert
```java
@Entity
@DynamicInsert  // null이 아닌 필드만 INSERT
public class Member {
    @Id
    private Long id;
    private String name;
    private String email;
    @ColumnDefault("'010-0000-0000'")  // DB 기본값 활용 가능
    private String phone;
    @ColumnDefault("'서울'")
    private String address;
}
```

**실제 값이 있는 필드만 INSERT:**
```java
Member member = Member.builder()
    .name("김철수")
    .email("kim@example.com")
    .build();
// phone, address는 null
```

**생성되는 SQL:**
```sql
INSERT INTO member (id, name, email)
VALUES (?, ?, ?)
-- phone, address는 DB 기본값 사용
```

#### @DynamicUpdate
```java
@Entity
@DynamicUpdate  // 실제 변경된 필드만 UPDATE
public class Member {
    // 필드 정의...
}
```

**변경 감지 시:**
```java
Member member = memberRepository.findById(1L);
member.setEmail("new@example.com");
// name, phone, address는 변경 없음
```

**생성되는 SQL:**
```sql
UPDATE member
SET email = ?
WHERE id = ?
-- 변경된 email만 update
```

### 3️⃣ 장단점 비교

| 구분 | 기본 방식 (Static SQL) | @DynamicInsert/@DynamicUpdate |
|------|----------------------|-------------------------------|
| **장점** | • SQL 캐싱으로 성능 향상<br>• PreparedStatement 재사용<br>• 예측 가능한 쿼리<br>• 디버깅 용이 | • 네트워크 트래픽 감소<br>• DB 기본값 활용 가능<br>• 불필요한 UPDATE 방지<br>• 트리거 최적화 |
| **단점** | • 모든 컬럼 전송 (트래픽↑)<br>• DB 기본값 무시<br>• 불필요한 UPDATE 발생 | • 매번 SQL 생성 (CPU↑)<br>• PreparedStatement 캐싱 불가<br>• 성능 오버헤드 존재 |

### 4️⃣ 언제 사용해야 할까?

#### ✅ @DynamicInsert 사용 권장 케이스
1. **많은 컬럼 + DB 기본값 활용**
   ```java
   @Entity
   @DynamicInsert
   public class Article {
       @Id private Long id;
       private String title;
       private String content;
       @ColumnDefault("0") private Integer viewCount;
       @ColumnDefault("0") private Integer likeCount;
       @ColumnDefault("'DRAFT'") private String status;
       @ColumnDefault("CURRENT_TIMESTAMP") private LocalDateTime createdAt;
       // 20개 이상의 선택적 필드들...
   }
   ```

2. **대용량 텍스트/BLOB 필드가 있는 경우**
   ```java
   @Entity
   @DynamicInsert
   public class Document {
       @Id private Long id;
       private String title;
       @Lob private String content;  // 대용량 텍스트
       @Lob private byte[] attachment;  // 대용량 바이너리
   }
   ```

#### ✅ @DynamicUpdate 사용 권장 케이스
1. **컬럼이 매우 많은 테이블 (30개 이상)**
2. **특정 필드만 자주 업데이트되는 경우**
   ```java
   @Entity
   @DynamicUpdate
   public class User {
       @Id private Long id;
       private String lastLoginTime;  // 자주 변경
       private String name;           // 거의 변경 없음
       private String ssn;            // 절대 변경 없음
       // 50개의 추가 필드들...
   }
   ```

3. **DB 트리거가 있는 경우**
   - 특정 컬럼 UPDATE 시에만 트리거 실행 필요

#### ❌ 사용하지 말아야 할 경우
1. **컬럼이 적은 테이블 (10개 이하)**
2. **대부분의 필드가 함께 변경되는 경우**
3. **성능이 중요한 대량 처리 시스템**

---

## 2. Rest Docs vs Swagger 비교 분석

### 1️⃣ Rest Docs란?

**Spring REST Docs**는 테스트 기반의 API 문서화 도구입니다.

#### 핵심 특징
- **테스트 기반**: 실제 테스트를 통과해야 문서 생성
- **AsciiDoc 형식**: 마크다운보다 강력한 문서 포맷
- **정적 HTML**: 빌드 시 HTML 파일 생성

#### 동작 원리
```java
@Test
void createMember() throws Exception {
    mockMvc.perform(post("/api/members")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"홍길동\",\"email\":\"hong@example.com\"}"))
        .andExpect(status().isCreated())
        .andDo(document("create-member",  // 문서화
            requestFields(
                fieldWithPath("name").description("회원 이름"),
                fieldWithPath("email").description("이메일 주소")
            ),
            responseFields(
                fieldWithPath("id").description("회원 ID"),
                fieldWithPath("name").description("회원 이름"),
                fieldWithPath("email").description("이메일 주소")
            )
        ));
}
```

### 2️⃣ Swagger vs Rest Docs 상세 비교

| 항목 | Swagger (springdoc-openapi) | Spring REST Docs |
|------|---------------------------|------------------|
| **문서 생성 방식** | 어노테이션 기반<br>`@Operation`, `@Schema` | 테스트 코드 기반<br>`MockMvc` + `document()` |
| **신뢰성** | 코드와 문서 불일치 가능 | 테스트 통과 = 문서 정확성 보장 |
| **UI/UX** | 인터랙티브 UI<br>API 직접 테스트 가능 | 정적 HTML<br>읽기 전용 문서 |
| **학습 곡선** | 낮음 (어노테이션만 추가) | 높음 (테스트 코드 작성 필수) |
| **초기 설정** | 간단 (의존성 추가만) | 복잡 (테스트 + 빌드 설정) |
| **유지보수** | 프로덕션 코드에 어노테이션 산재 | 테스트 코드에 문서화 집중 |
| **버전 관리** | 런타임 버전 전환 가능 | 빌드 시점 버전 고정 |
| **커스터마이징** | 제한적 (테마 정도) | 완전한 커스터마이징 가능 |

### 3️⃣ 장단점 심화 분석

#### Swagger의 장단점

**장점:**
1. **즉각적인 피드백**: 코드 작성 즉시 문서 확인
2. **Try it out**: 브라우저에서 API 직접 테스트
3. **낮은 진입 장벽**: 어노테이션만 추가하면 됨
4. **동적 문서**: 런타임에 문서 생성

**단점:**
1. **프로덕션 코드 오염**: 비즈니스 로직과 문서화 코드 혼재
2. **신뢰성 이슈**: 실제 동작과 문서가 다를 수 있음
3. **보안 우려**: 프로덕션 환경 노출 시 API 구조 노출

**실제 코드 예시 (현재 프로젝트):**
```java
@Operation(
    summary = "가게에 리뷰 추가하기",
    description = """
        가게에 새로운 리뷰를 작성합니다.

        **Validation:**
        - storeId: 가게 ID 필수, DB에 존재해야 함
        - memberId: 회원 ID 필수, DB에 존재해야 함
        - content: 리뷰 내용 필수, 10자 이상 500자 이하
        - star: 별점 필수, 0.0 ~ 5.0 범위
        """
)
@PostMapping
public ApiResponse<ReviewResponse.CreateReview> createReview(
    @Valid @RequestBody ReviewRequest.CreateReviewDTO request
) {
    // 비즈니스 로직
}
```

#### Rest Docs의 장단점

**장점:**
1. **100% 신뢰성**: 테스트 통과 = 문서 정확성
2. **깔끔한 코드**: 프로덕션 코드에서 문서화 분리
3. **강력한 포맷**: AsciiDoc의 풍부한 기능
4. **보안**: 정적 파일로 별도 호스팅 가능

**단점:**
1. **높은 학습 곡선**: 테스트 코드 작성 능력 필요
2. **즉각성 부족**: 빌드해야 문서 확인 가능
3. **인터랙티브 기능 없음**: API 테스트 불가
4. **초기 설정 복잡**: Gradle/Maven 설정 필요

**Rest Docs 코드 예시:**
```java
@Test
void createReview() throws Exception {
    // Given
    ReviewRequest.CreateReviewDTO request = ReviewRequest.CreateReviewDTO.builder()
        .storeId(1L)
        .memberId(1L)
        .content("정말 맛있어요! 재방문 의사 있습니다.")
        .star(4.5f)
        .build();

    // When & Then
    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andDo(document("create-review",
            requestFields(
                fieldWithPath("storeId").type(NUMBER)
                    .description("가게 ID"),
                fieldWithPath("memberId").type(NUMBER)
                    .description("회원 ID"),
                fieldWithPath("content").type(STRING)
                    .description("리뷰 내용 (10-500자)"),
                fieldWithPath("star").type(NUMBER)
                    .description("별점 (0.0-5.0)")
            ),
            responseFields(
                fieldWithPath("isSuccess").description("성공 여부"),
                fieldWithPath("code").description("응답 코드"),
                fieldWithPath("message").description("응답 메시지"),
                fieldWithPath("data.reviewId").description("생성된 리뷰 ID"),
                fieldWithPath("data.createdAt").description("생성 시간")
            )
        ));
}
```

### 4️⃣ 언제 어떤 도구를 선택해야 할까?

#### ✅ Swagger 선택 기준

**적합한 경우:**
1. **스타트업/MVP 단계**
   - 빠른 개발과 문서화 필요
   - 프론트엔드와 즉각적인 협업

2. **내부 API/마이크로서비스**
   - 개발팀 간 빠른 소통
   - Try it out 기능 활용

3. **리소스 제약**
   - 테스트 코드 작성 여력 부족
   - 빠른 온보딩 필요

**현재 프로젝트가 Swagger를 선택한 이유:**
- 학습 프로젝트로 빠른 개발 우선
- 팀원들의 테스트 코드 경험 부족
- 즉각적인 API 테스트 필요

#### ✅ Rest Docs 선택 기준

**적합한 경우:**
1. **엔터프라이즈/금융 시스템**
   - 문서 정확성이 절대적으로 중요
   - 규제 준수 필요

2. **공개 API/SDK**
   - 외부 개발자 대상
   - 전문적인 문서 필요

3. **테스트 문화 정착**
   - TDD 실천 중
   - 높은 테스트 커버리지

**Rest Docs 도입 시 고려사항:**
```groovy
// build.gradle 설정 예시
plugins {
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
}

dependencies {
    testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
}

test {
    outputs.dir snippetsDir
}

asciidoctor {
    inputs.dir snippetsDir
    dependsOn test
}
```

---

## 3. 실전 적용 가이드

### 현재 프로젝트 개선 제안

#### 1. @DynamicUpdate 적용 검토

**적용 대상: Review 엔티티**
```java
@Entity
@DynamicUpdate  // 추가
public class Review extends BaseEntity {
    // 리뷰는 주로 star와 content만 수정됨
    // 나머지 필드는 거의 변경 없음
}
```

#### 2. 점진적 Rest Docs 도입

**1단계: 핵심 API만 Rest Docs 적용**
- 외부 공개 예정 API
- 자주 변경되는 API

**2단계: Swagger와 병행 운영**
- 개발 환경: Swagger (빠른 테스트)
- 프로덕션 문서: Rest Docs (정확성)

### 성능 테스트 결과 (예상)

#### @DynamicInsert 성능 비교
| 시나리오 | 기본 방식 | @DynamicInsert | 개선율 |
|---------|----------|---------------|-------|
| 5개 컬럼, 2개만 값 | 12ms | 15ms | -25% |
| 30개 컬럼, 5개만 값 | 45ms | 32ms | +29% |
| 100개 컬럼, 10개만 값 | 120ms | 65ms | +46% |

#### @DynamicUpdate 성능 비교
| 시나리오 | 기본 방식 | @DynamicUpdate | 개선율 |
|---------|----------|---------------|-------|
| 5개 컬럼, 1개 수정 | 10ms | 13ms | -30% |
| 30개 컬럼, 2개 수정 | 38ms | 25ms | +34% |
| 100개 컬럼, 3개 수정 | 95ms | 40ms | +58% |

**결론: 컬럼이 많을수록 Dynamic 방식이 유리**

### 최종 권장사항

1. **@DynamicInsert/@DynamicUpdate**
   - 현재 프로젝트는 컬럼이 적어 불필요
   - 향후 컬럼이 30개 이상으로 늘어나면 검토

2. **API 문서화**
   - 현재: Swagger 유지 (학습 단계)
   - 중장기: Rest Docs 도입 검토
   - 이상적: 두 도구 병행 사용

---

## 📎 참고 자료
- [Hibernate Dynamic SQL Generation](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#entity-dynamic-sql)
- [Spring REST Docs 공식 문서](https://docs.spring.io/spring-restdocs/docs/current/reference/html5/)
- [springdoc-openapi 공식 문서](https://springdoc.org/)

---

*작성일: 2024-12-01*
*작성자: UMC 9기 Spring Boot Study*