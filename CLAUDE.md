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
- **JPA Settings**:
  - `ddl-auto: update` - Schema auto-updates on application start
  - `show-sql: true` - SQL queries logged to console
  - Auditing enabled via `@EnableJpaAuditing` in main application class

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
└── entity/     - Shared base entities
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
- **converter/** - Entity-DTO conversion utilities

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

All time-tracked entities should extend `BaseEntity`:
- Provides automatic `createdAt` and `updatedAt` timestamps
- Uses Spring Data JPA's `@CreatedDate` and `@LastModifiedDate`
- Requires `@EnableJpaAuditing` in main application class (already configured)

#### 3. Lombok Conventions

All entities follow this Lombok pattern:
```java
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA requirement
@AllArgsConstructor(access = AccessLevel.PROTECTED) // or PRIVATE for non-extendable
@Getter
```

This prevents external instantiation while supporting JPA proxies and the builder pattern.

## Development Guidelines

### Adding New Entities

1. Create entity class in appropriate `domain/[domain-name]/entity/` package
2. Extend `BaseEntity` if timestamps are needed
3. Use appropriate fetch strategies (`LAZY` preferred for associations)
4. Create corresponding repository with fetch join queries for N+1 prevention
5. Add enum types in `domain/[domain-name]/enums/` if needed

### Working with Relationships

- Always use `FetchType.LAZY` for `@OneToMany` and `@ManyToOne`
- Initialize collections with `@Builder.Default` and `new ArrayList<>()`
- For bidirectional relationships, maintain both sides of the association
- Use fetch joins in repository queries when accessing related entities

### Enum Usage

Store enums as strings in the database using `@Enumerated(EnumType.STRING)` for better readability and database migration safety.

## Technology Stack

- **Spring Boot**: 3.5.6
- **Java**: 21 (with toolchain support)
- **Build Tool**: Gradle
- **Database**: MySQL with MySQL Connector/J
- **ORM**: Spring Data JPA with Hibernate
- **Utility**: Lombok for boilerplate reduction
- **Testing**: JUnit 5 with Spring Boot Test
