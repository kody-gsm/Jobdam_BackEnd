package com.example.kodyjobdam.recruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 선생님이 올린 채용 공고 이미지 한 장 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recruit_image")
public class RecruitImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_id")
    private RecruitEntity recruit;

    /** 공고 안에서의 이미지 순서 (1부터 시작). 이미지를 지우면 중간 번호가 비어도 순서만 맞으면 된다. */
    private int orderIndex;

    /** 공개 조회 URL. 로그인 없이도 접근할 수 있다. */
    private String imageUrl;

    void assignRecruit(RecruitEntity recruit) {
        this.recruit = recruit;
    }
}
