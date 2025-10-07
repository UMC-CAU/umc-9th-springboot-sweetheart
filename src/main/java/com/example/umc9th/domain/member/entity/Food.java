package com.example.umc9th.domain.member.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.umc9th.domain.member.entity.mapping.MemberFood;
import com.example.umc9th.domain.member.enums.FoodName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "food")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @Enumerated(EnumType.STRING)
    private FoodName name;

    // 양방향 관계: 이 음식을 선호하는 회원 목록
    @OneToMany(mappedBy = "food", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberFood> memberFoodList = new ArrayList<>();
}
