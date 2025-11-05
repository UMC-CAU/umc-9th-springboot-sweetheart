package com.example.umc9th.domain.member.repository;

import com.example.umc9th.domain.member.entity.Member;
import com.example.umc9th.domain.member.enums.Gender;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.example.umc9th.domain.member.entity.QMember.member;
import static com.example.umc9th.domain.member.entity.mapping.QMemberFood.memberFood;
import static com.example.umc9th.domain.member.entity.QFood.food;

/**
 * QueryDSL을 사용한 Member 동적 쿼리 Repository
 * 복잡한 검색 조건, N+1 문제 해결, 동적 필터링 등을 처리합니다.
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 회원 동적 검색 (N+1 문제 해결 포함)
     *
     * @param name 회원 이름 (부분 일치 검색, null이면 조건 무시)
     * @param gender 성별 (null이면 조건 무시)
     * @param minAge 최소 나이 (null이면 조건 무시)
     * @return 검색 조건에 맞는 회원 리스트 (선호 음식 정보 포함)
     */
    public List<Member> searchMembers(String name, Gender gender, Integer minAge) {
        return queryFactory
                .selectFrom(member)
                .distinct()
                // N+1 문제 방지: Fetch Join으로 한 번에 조회
                .leftJoin(member.memberFoodList, memberFood).fetchJoin()
                .leftJoin(memberFood.food, food).fetchJoin()
                .where(
                        nameContains(name),
                        genderEq(gender),
                        ageGoe(minAge)
                )
                .fetch();
    }

    /**
     * 특정 음식을 선호하는 회원 조회
     *
     * @param foodId 음식 ID
     * @return 해당 음식을 선호하는 회원 리스트
     */
    public List<Member> findMembersByFoodId(Long foodId) {
        return queryFactory
                .selectFrom(member)
                .distinct()
                .join(member.memberFoodList, memberFood)
                .join(memberFood.food, food)
                .where(food.id.eq(foodId))
                .fetch();
    }

    /**
     * 생년월일 범위로 회원 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 기간에 태어난 회원 리스트
     */
    public List<Member> findMembersByBirthBetween(LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(member)
                .where(member.birth.between(startDate, endDate))
                .orderBy(member.birth.asc())
                .fetch();
    }

    /**
     * 특정 주소에 거주하는 회원 수 조회
     *
     * @param address 주소 (부분 일치)
     * @return 해당 주소에 거주하는 회원 수
     */
    public Long countMembersByAddress(String address) {
        return queryFactory
                .select(member.count())
                .from(member)
                .where(addressContains(address))
                .fetchOne();
    }

    // ========== 동적 조건 메서드 (BooleanExpression) ==========

    /**
     * 이름 부분 일치 조건 (null이면 조건 무시)
     */
    private BooleanExpression nameContains(String name) {
        return name != null ? member.name.contains(name) : null;
    }

    /**
     * 성별 일치 조건 (null이면 조건 무시)
     */
    private BooleanExpression genderEq(Gender gender) {
        return gender != null ? member.gender.eq(gender) : null;
    }

    /**
     * 최소 나이 조건 (null이면 조건 무시)
     * 나이 = 현재 년도 - 생년월일 년도 + 1
     */
    private BooleanExpression ageGoe(Integer minAge) {
        if (minAge == null) {
            return null;
        }
        int currentYear = LocalDate.now().getYear();
        int maxBirthYear = currentYear - minAge + 1;
        return member.birth.year().loe(maxBirthYear);
    }

    /**
     * 주소 부분 일치 조건 (null이면 조건 무시)
     */
    private BooleanExpression addressContains(String address) {
        return address != null ? member.address.contains(address) : null;
    }
}
