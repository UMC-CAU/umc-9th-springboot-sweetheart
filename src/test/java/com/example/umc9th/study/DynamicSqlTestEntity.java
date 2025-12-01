package com.example.umc9th.study;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @DynamicInsert/@DynamicUpdate 테스트용 엔티티
 *
 * 이 엔티티는 Dynamic SQL 생성의 효과를 테스트하기 위해 만들어졌습니다.
 * 많은 컬럼을 가진 테이블을 시뮬레이션합니다.
 */
@Entity
@Table(name = "test_dynamic_entity")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert  // null이 아닌 필드만 INSERT
@DynamicUpdate  // 실제 변경된 필드만 UPDATE
public class DynamicSqlTestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requiredField;  // 필수 필드

    // 많은 선택적 필드들 (실제 엔터프라이즈 시스템 시뮬레이션)
    @ColumnDefault("'DEFAULT_VALUE_1'")
    private String optionalField1;

    @ColumnDefault("'DEFAULT_VALUE_2'")
    private String optionalField2;

    @ColumnDefault("'DEFAULT_VALUE_3'")
    private String optionalField3;

    @ColumnDefault("'DEFAULT_VALUE_4'")
    private String optionalField4;

    @ColumnDefault("'DEFAULT_VALUE_5'")
    private String optionalField5;

    @ColumnDefault("0")
    private Integer counter1;

    @ColumnDefault("0")
    private Integer counter2;

    @ColumnDefault("0")
    private Integer counter3;

    @ColumnDefault("0.0")
    private Double score1;

    @ColumnDefault("0.0")
    private Double score2;

    @ColumnDefault("0.0")
    private Double score3;

    @ColumnDefault("false")
    private Boolean flag1;

    @ColumnDefault("false")
    private Boolean flag2;

    @ColumnDefault("false")
    private Boolean flag3;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String largeText1;  // 대용량 텍스트 필드

    @Lob
    @Column(columnDefinition = "TEXT")
    private String largeText2;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String largeText3;

    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime timestamp1;

    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime timestamp2;

    // 자주 업데이트되는 필드
    private LocalDateTime lastModifiedTime;

    // 거의 업데이트되지 않는 필드
    private String rarelyChangedField;
}