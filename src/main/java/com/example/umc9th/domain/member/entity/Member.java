package com.example.umc9th.domain.member.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.umc9th.domain.member.entity.mapping.MemberFood;
import com.example.umc9th.domain.member.entity.mapping.MemberTerm;
import com.example.umc9th.domain.member.enums.Gender;
import com.example.umc9th.global.auth.enums.SocialType;
import com.example.umc9th.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Member 엔티티
 *
 * @DynamicUpdate 적용 이유:
 * - updateInfo() 메서드: 일부 필드만 선택적 업데이트
 * - addPoints(), deductPoints(): point 필드만 빈번하게 업데이트
 * - 부분 업데이트가 많아 Dynamic SQL이 효과적
 *
 * 성능 개선 예상:
 * - 포인트 업데이트 시: 2개 필드만 UPDATE (vs 기본 14개)
 * - 전화번호 변경 시: 2개 필드만 UPDATE (vs 기본 14개)
 */
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "member")
@DynamicUpdate  // 실제 변경된 필드만 UPDATE 쿼리에 포함
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Gender gender = Gender.NONE;

    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address", nullable = false)
    private String detailAddress;

    @Column(name = "social_uid", nullable = false)
    private String socialUid;

    @Column(name = "social_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Column(name = "point", nullable = false)
    private Integer point;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    // 양방향 관계: Member가 선호하는 음식 목록
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberFood> memberFoodList = new ArrayList<>();

    // 양방향 관계: Member가 동의한 약관 목록
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberTerm> memberTermList = new ArrayList<>();

    /**
     * 회원 정보 수정 메서드
     * JPA 더티 체킹을 활용하여 변경 감지 후 자동으로 UPDATE 쿼리 실행
     *
     * @param name 이름 (null이면 변경 안 함)
     * @param email 이메일 (null이면 변경 안 함)
     * @param phoneNumber 전화번호 (null이면 변경 안 함)
     * @param address 주소 (null이면 변경 안 함)
     * @param detailAddress 상세 주소 (null이면 변경 안 함)
     */
    public void updateInfo(String name, String email, String phoneNumber, String address, String detailAddress) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
        if (detailAddress != null && !detailAddress.isBlank()) {
            this.detailAddress = detailAddress;
        }
    }

    /**
     * 포인트 추가 메서드
     *
     * @param points 추가할 포인트
     */
    public void addPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다");
        }
        this.point += points;
    }

    /**
     * 포인트 차감 메서드
     *
     * @param points 차감할 포인트
     */
    public void deductPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다");
        }
        if (this.point < points) {
            throw new IllegalArgumentException("포인트가 부족합니다");
        }
        this.point -= points;
    }

}