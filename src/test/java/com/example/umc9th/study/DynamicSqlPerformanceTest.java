package com.example.umc9th.study;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @DynamicInsert/@DynamicUpdate 성능 테스트
 *
 * 이 테스트는 Dynamic SQL과 Static SQL의 실제 성능 차이를 측정합니다.
 *
 * 테스트 시나리오:
 * 1. 적은 필드만 설정한 INSERT - Dynamic이 유리할 것으로 예상
 * 2. 모든 필드를 설정한 INSERT - Static이 유리할 것으로 예상
 * 3. 단일 필드 UPDATE - Dynamic이 유리할 것으로 예상
 * 4. 모든 필드 UPDATE - Static이 유리할 것으로 예상
 */
@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Dynamic SQL 성능 테스트")
class DynamicSqlPerformanceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    private static final int TEST_ITERATIONS = 100;  // 테스트 반복 횟수

    @BeforeEach
    void setUp() {
        // Hibernate Statistics 활성화
        Session session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @Order(1)
    @DisplayName("1. INSERT 성능 비교 - 적은 필드만 설정")
    @Transactional
    @Rollback(false)
    void testInsertPerformanceWithFewFields() {
        log.info("========== INSERT 성능 테스트 (적은 필드) ==========");

        // Dynamic Insert 테스트
        long dynamicStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity dynamicEntity = DynamicSqlTestEntity.builder()
                    .requiredField("Required " + i)
                    .optionalField1("Optional " + i)  // 2개 필드만 설정
                    .counter1(i)
                    .build();
            testEntityManager.persist(dynamicEntity);
            if (i % 20 == 0) {
                testEntityManager.flush();
                testEntityManager.clear();
            }
        }
        testEntityManager.flush();
        long dynamicTime = System.currentTimeMillis() - dynamicStartTime;

        // Static Insert 테스트
        long staticStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity.StaticSqlTestEntity staticEntity =
                    DynamicSqlTestEntity.StaticSqlTestEntity.builder()
                    .requiredField("Required " + i)
                    .optionalField1("Optional " + i)  // 2개 필드만 설정
                    .counter1(i)
                    .build();
            testEntityManager.persist(staticEntity);
            if (i % 20 == 0) {
                testEntityManager.flush();
                testEntityManager.clear();
            }
        }
        testEntityManager.flush();
        long staticTime = System.currentTimeMillis() - staticStartTime;

        // 결과 출력
        log.info("📊 INSERT 성능 비교 ({}건, 적은 필드)", TEST_ITERATIONS);
        log.info("  - Dynamic Insert: {}ms", dynamicTime);
        log.info("  - Static Insert: {}ms", staticTime);
        log.info("  - 성능 개선율: {}%", calculateImprovement(staticTime, dynamicTime));
        log.info("  - SQL 생성 횟수: {}", statistics.getPrepareStatementCount());

        // Dynamic이 더 빨라야 함 (적은 필드만 INSERT)
        if (dynamicTime < staticTime) {
            log.info("✅ Dynamic Insert가 더 빠름 (예상대로)");
        } else {
            log.warn("⚠️ Static Insert가 더 빠름 (예상과 다름)");
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. INSERT 성능 비교 - 모든 필드 설정")
    @Transactional
    @Rollback(false)
    void testInsertPerformanceWithAllFields() {
        log.info("========== INSERT 성능 테스트 (모든 필드) ==========");

        // Dynamic Insert 테스트
        long dynamicStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity dynamicEntity = createFullEntity(i);
            testEntityManager.persist(dynamicEntity);
            if (i % 20 == 0) {
                testEntityManager.flush();
                testEntityManager.clear();
            }
        }
        testEntityManager.flush();
        long dynamicTime = System.currentTimeMillis() - dynamicStartTime;

        // Static Insert 테스트
        long staticStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity.StaticSqlTestEntity staticEntity = createFullStaticEntity(i);
            testEntityManager.persist(staticEntity);
            if (i % 20 == 0) {
                testEntityManager.flush();
                testEntityManager.clear();
            }
        }
        testEntityManager.flush();
        long staticTime = System.currentTimeMillis() - staticStartTime;

        // 결과 출력
        log.info("📊 INSERT 성능 비교 ({}건, 모든 필드)", TEST_ITERATIONS);
        log.info("  - Dynamic Insert: {}ms", dynamicTime);
        log.info("  - Static Insert: {}ms", staticTime);
        log.info("  - 성능 차이: {}%", calculateImprovement(staticTime, dynamicTime));

        // Static이 더 빨라야 함 (모든 필드 INSERT, SQL 캐싱 효과)
        if (staticTime < dynamicTime) {
            log.info("✅ Static Insert가 더 빠름 (예상대로)");
        } else {
            log.warn("⚠️ Dynamic Insert가 더 빠름 (예상과 다름)");
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. UPDATE 성능 비교 - 단일 필드 변경")
    @Transactional
    void testUpdatePerformanceSingleField() {
        log.info("========== UPDATE 성능 테스트 (단일 필드) ==========");

        // 테스트 데이터 준비
        DynamicSqlTestEntity dynamicEntity = createFullEntity(1);
        dynamicEntity = testEntityManager.persistAndFlush(dynamicEntity);
        Long dynamicId = dynamicEntity.getId();

        DynamicSqlTestEntity.StaticSqlTestEntity staticEntity = createFullStaticEntity(1);
        staticEntity = testEntityManager.persistAndFlush(staticEntity);
        Long staticId = staticEntity.getId();

        testEntityManager.clear();

        // Dynamic Update 테스트
        long dynamicStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity entity = testEntityManager.find(DynamicSqlTestEntity.class, dynamicId);
            entity.setLastModifiedTime(LocalDateTime.now());  // 단일 필드만 변경
            testEntityManager.flush();
            testEntityManager.clear();
        }
        long dynamicTime = System.currentTimeMillis() - dynamicStartTime;

        // Static Update 테스트
        long staticStartTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DynamicSqlTestEntity.StaticSqlTestEntity entity =
                testEntityManager.find(DynamicSqlTestEntity.StaticSqlTestEntity.class, staticId);
            entity.setLastModifiedTime(LocalDateTime.now());  // 단일 필드만 변경
            testEntityManager.flush();
            testEntityManager.clear();
        }
        long staticTime = System.currentTimeMillis() - staticStartTime;

        // 결과 출력
        log.info("📊 UPDATE 성능 비교 ({}건, 단일 필드)", TEST_ITERATIONS);
        log.info("  - Dynamic Update: {}ms", dynamicTime);
        log.info("  - Static Update: {}ms", staticTime);
        log.info("  - 성능 개선율: {}%", calculateImprovement(staticTime, dynamicTime));

        // Dynamic이 더 빨라야 함 (단일 필드만 UPDATE)
        if (dynamicTime < staticTime) {
            log.info("✅ Dynamic Update가 더 빠름 (예상대로)");
        } else {
            log.warn("⚠️ Static Update가 더 빠름 (예상과 다름)");
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. SQL 생성 패턴 분석")
    @Transactional
    void analyzeSqlGenerationPattern() {
        log.info("========== SQL 생성 패턴 분석 ==========");

        // Dynamic Entity - 적은 필드로 INSERT
        log.info("\n📝 Dynamic Insert (적은 필드):");
        DynamicSqlTestEntity dynamicEntity1 = DynamicSqlTestEntity.builder()
                .requiredField("Test")
                .optionalField1("Option1")
                .build();
        testEntityManager.persistAndFlush(dynamicEntity1);
        // 실제 SQL: INSERT INTO test_dynamic_entity (required_field, optional_field1) VALUES (?, ?)

        // Static Entity - 적은 필드로 INSERT
        log.info("\n📝 Static Insert (적은 필드):");
        DynamicSqlTestEntity.StaticSqlTestEntity staticEntity1 =
                DynamicSqlTestEntity.StaticSqlTestEntity.builder()
                .requiredField("Test")
                .optionalField1("Option1")
                .build();
        testEntityManager.persistAndFlush(staticEntity1);
        // 실제 SQL: INSERT INTO test_static_entity (required_field, optional_field1, optional_field2,
        //            optional_field3, ... 모든 컬럼) VALUES (?, ?, ?, ?, ...)

        testEntityManager.clear();

        // UPDATE 패턴 분석
        log.info("\n📝 Dynamic Update (단일 필드):");
        DynamicSqlTestEntity dynamicForUpdate =
                testEntityManager.find(DynamicSqlTestEntity.class, dynamicEntity1.getId());
        dynamicForUpdate.setLastModifiedTime(LocalDateTime.now());
        testEntityManager.flush();
        // 실제 SQL: UPDATE test_dynamic_entity SET last_modified_time = ? WHERE id = ?

        log.info("\n📝 Static Update (단일 필드):");
        DynamicSqlTestEntity.StaticSqlTestEntity staticForUpdate =
                testEntityManager.find(DynamicSqlTestEntity.StaticSqlTestEntity.class, staticEntity1.getId());
        staticForUpdate.setLastModifiedTime(LocalDateTime.now());
        testEntityManager.flush();
        // 실제 SQL: UPDATE test_static_entity SET required_field = ?, optional_field1 = ?,
        //            ... 모든 컬럼 = ? WHERE id = ?

        log.info("\n📊 Statistics 요약:");
        log.info("  - 전체 쿼리 실행 횟수: {}", statistics.getQueryExecutionCount());
        log.info("  - PreparedStatement 생성 횟수: {}", statistics.getPrepareStatementCount());
        log.info("  - Entity 로드 횟수: {}", statistics.getEntityLoadCount());
        log.info("  - Entity 업데이트 횟수: {}", statistics.getEntityUpdateCount());
    }

    // Helper 메서드들
    private DynamicSqlTestEntity createFullEntity(int index) {
        return DynamicSqlTestEntity.builder()
                .requiredField("Required " + index)
                .optionalField1("Optional1 " + index)
                .optionalField2("Optional2 " + index)
                .optionalField3("Optional3 " + index)
                .optionalField4("Optional4 " + index)
                .optionalField5("Optional5 " + index)
                .counter1(index)
                .counter2(index * 2)
                .counter3(index * 3)
                .score1(index * 1.1)
                .score2(index * 2.2)
                .score3(index * 3.3)
                .flag1(index % 2 == 0)
                .flag2(index % 3 == 0)
                .flag3(index % 5 == 0)
                .largeText1("Large text content " + index)
                .largeText2("Another large text " + index)
                .largeText3("Third large text " + index)
                .timestamp1(LocalDateTime.now())
                .timestamp2(LocalDateTime.now())
                .lastModifiedTime(LocalDateTime.now())
                .rarelyChangedField("Rarely " + index)
                .build();
    }

    private DynamicSqlTestEntity.StaticSqlTestEntity createFullStaticEntity(int index) {
        return DynamicSqlTestEntity.StaticSqlTestEntity.builder()
                .requiredField("Required " + index)
                .optionalField1("Optional1 " + index)
                .optionalField2("Optional2 " + index)
                .optionalField3("Optional3 " + index)
                .optionalField4("Optional4 " + index)
                .optionalField5("Optional5 " + index)
                .counter1(index)
                .counter2(index * 2)
                .counter3(index * 3)
                .score1(index * 1.1)
                .score2(index * 2.2)
                .score3(index * 3.3)
                .flag1(index % 2 == 0)
                .flag2(index % 3 == 0)
                .flag3(index % 5 == 0)
                .largeText1("Large text content " + index)
                .largeText2("Another large text " + index)
                .largeText3("Third large text " + index)
                .timestamp1(LocalDateTime.now())
                .timestamp2(LocalDateTime.now())
                .lastModifiedTime(LocalDateTime.now())
                .rarelyChangedField("Rarely " + index)
                .build();
    }

    private double calculateImprovement(long baseline, long improved) {
        if (baseline == 0) return 0;
        return Math.round(((double)(baseline - improved) / baseline * 100) * 100.0) / 100.0;
    }

    @AfterEach
    void tearDown() {
        if (statistics != null) {
            statistics.setStatisticsEnabled(false);
        }
    }
}