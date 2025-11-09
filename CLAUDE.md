# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 application built with Java 21, using Gradle as the build tool. It's a UMC (University MakeUs Challenge) 9th generation project focused on learning Spring Boot with JPA, MySQL, and RESTful API development.

## Build & Run Commands

### Build the project
```bash
./gradlew build
```

### Run the application
```bash
./gradlew bootRun
```

### Run tests
```bash
./gradlew test
```

### Run a single test class
```bash
./gradlew test --tests "com.example.umc9th.ClassName"
```

### Clean build artifacts
```bash
./gradlew clean
```

## Database Configuration

- **Database**: MySQL
- **Connection**: Configured via environment variables in `application.yml`
  - `DB_URL`: JDBC connection URL (e.g., `jdbc:mysql://localhost:3306/umc9th`)
  - `DB_USER`: MySQL username
  - `DB_PW`: MySQL password
- **JPA Settings** (Profile별 다름):
  - **Local** (`application-local.yml`):
    - `ddl-auto: update` - 엔티티 변경 시 자동 반영 (개발 편의)
    - `show-sql: true` - SQL 쿼리 로그 활성화 (학습용)
  - **Prod** (`application-prod.yml`):
    - `ddl-auto: validate` - 스키마 검증만, 자동 변경 금지 (안전)
    - `show-sql: false` - SQL 로그 비활성화 (성능)
- **Auditing**: `@EnableJpaAuditing` in main application class

## Architecture & Project Structure

### Domain-Driven Design Layout

The project follows a domain-driven structure under `com.example.umc9th`:

```
domain/
├── member/     - Member management domain
├── mission/    - Mission domain (placeholder)
├── review/     - Review domain (placeholder)
└── store/      - Store domain (placeholder)

global/
├── entity/     - Shared base entities
├── response/   - Unified API response structure
├── exception/  - Global exception handling
└── config/     - Application configuration
```

### Layer Organization (within each domain)

Each domain module follows this standard layered structure:

- **entity/** - JPA entities and mapping classes
  - **mapping/** - Join table entities for many-to-many relationships
- **enums/** - Enum types for domain-specific constants
- **repository/** - Spring Data JPA repositories
- **service/** - Business logic layer
- **controller/** - REST API endpoints
- **dto/** - Data transfer objects

### Entity Relationships

**Member Domain** (fully implemented):

- **Member** (주 엔티티)
  - Extends `BaseEntity` for automatic `createdAt`/`updatedAt` timestamps
  - Has bidirectional relationship with `Food` via `MemberFood` join table
  - Uses Lombok's `@Builder` pattern with protected constructors

- **Food** (음식 카테고리)
  - Uses `FoodName` enum: KOREAN, CHINESE, JAPANESE, WESTERN, ETC
  - Bidirectional relationship with `Member`

- **Term** (약관)
  - Uses `TermName` enum: AGE, SERVICE, PRIVACY, LOCATION, MARKETING
  - Unidirectional relationship (MemberTerm is a placeholder)

- **MemberFood** (매핑 엔티티)
  - Join table managing many-to-many between Member and Food
  - Uses `@ManyToOne(fetch = FetchType.LAZY)` to prevent eager loading

### Important Implementation Patterns

#### 1. N+1 Problem Prevention

The `MemberRepository` demonstrates comprehensive Fetch Join strategies to prevent N+1 query problems:

```java
// Recommended: Fetch Member with all related Food details in one query
@Query("SELECT DISTINCT m FROM Member m " +
       "LEFT JOIN FETCH m.memberFoodList mf " +
       "LEFT JOIN FETCH mf.food")
List<Member> findAllWithFoods();
```

**Key patterns implemented**:
- `findAllWithMemberFoods()` - 1-level fetch join (Member + MemberFood)
- `findAllWithFoods()` - 2-level fetch join (Member + MemberFood + Food) ⭐ Most commonly used
- `findByIdWithFoods(@Param("id") Long id)` - Single member with foods
- `findByNameWithFoods(@Param("name") String name)` - Query by name with foods

Always use these fetch join methods when accessing member food preferences to avoid N+1 problems.

#### 2. Base Entity Pattern

The codebase uses a two-tier base entity hierarchy:

- **BaseTimeEntity** (lowest level)
  - Provides `createdAt` and `updatedAt` timestamps
  - Uses Spring Data JPA's `@CreatedDate` and `@LastModifiedDate`
  - Requires `@EnableJpaAuditing` in main application class (already configured)

- **BaseEntity** (extends BaseTimeEntity)
  - Adds soft delete functionality via `deletedAt` field
  - Provides `softDelete()` method to mark entities as deleted
  - Provides `isDeleted()` method to check deletion status

All time-tracked entities should extend one of these base entities. Use `BaseEntity` if soft delete is needed, otherwise use `BaseTimeEntity`.

#### 3. Unified API Response Structure

All API responses follow a consistent structure using `ApiResponse<T>`:

**Success Response Format:**
```json
{
  "isSuccess": true,
  "code": "MEMBER_200",
  "message": "회원 조회 성공",
  "timestamp": "2024-01-15T10:30:00",
  "data": { ... }
}
```

**Error Response Format:**
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

**Usage in Controllers:**
```java
// Success with data
return ApiResponse.onSuccess(SuccessCode.MEMBER_OK, member);

// Success without data
return ApiResponse.onSuccess(SuccessCode.MEMBER_DELETED);

// Shorthand methods
return ApiResponse.ok(data);          // Uses SuccessCode.OK
return ApiResponse.created(data);     // Uses SuccessCode.CREATED
```

#### 4. Error Handling Architecture

**GlobalExceptionHandler** (`@RestControllerAdvice`) handles all exceptions consistently:

- **CustomException** - Business logic errors with custom error codes
- **MethodArgumentNotValidException** - Validation errors with field details
- **MissingServletRequestParameterException** - Missing required parameters
- **MethodArgumentTypeMismatchException** - Type conversion errors
- **NoHandlerFoundException** - 404 Not Found
- **Exception** - Catch-all for unexpected errors

**Custom Error Codes:**
- Defined in `ErrorCode` enum implementing `BaseCode` interface
- Format: `{DOMAIN}_{HTTP_STATUS}` (e.g., `MEMBER_404`, `COMMON_500`)
- Each code has status, code, and message

**Throwing Custom Exceptions:**
```java
throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
```

**Response Code Pattern:**
- All response codes (success and error) implement `BaseCode` interface
- Ensures consistency: `getStatus()`, `getCode()`, `getMessage()`
- Success codes in `SuccessCode` enum
- Error codes in `ErrorCode` enum

#### 5. Lombok Conventions

All entities follow this Lombok pattern:
```java
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA requirement
@AllArgsConstructor(access = AccessLevel.PROTECTED) // or PRIVATE for non-extendable
@Getter
```

This prevents external instantiation while supporting JPA proxies and the builder pattern.

#### 6. Service Layer Patterns

**Transaction Management:**
- Class-level `@Transactional(readOnly = true)` for read operations
- Method-level `@Transactional` for write operations (create, update, delete)

**Exception Handling:**
- Use `CustomException` with appropriate `ErrorCode` for business logic errors
- Example: `.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND))`

**Logging:**
- All service methods log entry points with relevant parameters
- Use SLF4J with pattern: `log.info("[ClassName.methodName] Description - param: {}", value)`

#### 7. API Documentation (Swagger)

- Swagger UI available at `/swagger-ui.html`
- API docs available at `/api-docs`
- Use `@Tag` for controller-level descriptions
- Use `@Operation` for endpoint-level descriptions
- Use `@Parameter` for parameter-level descriptions

**Configuration:**
- Operations and tags sorted alphabetically
- Request duration displayed
- Actuator endpoints hidden from Swagger

## Development Guidelines

### Adding New Entities

1. Create entity class in appropriate `domain/[domain-name]/entity/` package
2. Extend `BaseEntity` if timestamps and soft delete are needed, or `BaseTimeEntity` if only timestamps are needed
3. Use appropriate fetch strategies (`LAZY` preferred for associations)
4. Create corresponding repository with fetch join queries for N+1 prevention
5. Add enum types in `domain/[domain-name]/enums/` if needed

### Working with Relationships

- Always use `FetchType.LAZY` for `@OneToMany` and `@ManyToOne`
- Initialize collections with `@Builder.Default` and `new ArrayList<>()`
- For bidirectional relationships, maintain both sides of the association
- Use fetch joins in repository queries when accessing related entities

### Adding New API Endpoints

1. Create DTOs in `domain/[domain-name]/dto/` package
   - Nested static classes for different use cases (e.g., `Request.Create`, `Response.Detail`)
   - Use `static from(Entity entity)` factory methods for entity-to-DTO conversion
2. Implement service methods with proper transaction management
3. Create controller endpoints returning `ApiResponse<T>`
4. Use appropriate `SuccessCode` or `ErrorCode` enums
5. Add Swagger annotations for documentation

### Enum Usage

Store enums as strings in the database using `@Enumerated(EnumType.STRING)` for better readability and database migration safety.

## Technology Stack

- **Spring Boot**: 3.5.6
- **Java**: 21 (with toolchain support)
- **Build Tool**: Gradle
- **Database**: MySQL with MySQL Connector/J
- **ORM**: Spring Data JPA with Hibernate
- **API Documentation**: Springdoc OpenAPI (Swagger UI)
- **Monitoring**: Spring Boot Actuator
- **Utility**: Lombok for boilerplate reduction
- **Testing**: JUnit 5 with Spring Boot Test, H2 Database for test environment
