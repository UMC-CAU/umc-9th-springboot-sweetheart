package com.example.umc9th.domain.member.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.umc9th.domain.member.entity.mapping.MemberTerm;
import com.example.umc9th.domain.member.enums.TermName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "term")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @Enumerated(EnumType.STRING)
    private TermName name;

    // 양방향 관계: 이 약관에 동의한 회원 목록
    @OneToMany(mappedBy = "term", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberTerm> memberTermList = new ArrayList<>();
}
