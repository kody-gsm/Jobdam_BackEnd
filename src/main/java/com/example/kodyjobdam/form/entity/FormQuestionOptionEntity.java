package com.example.kodyjobdam.form.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "form_question_option")
public class FormQuestionOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private FormQuestionEntity question;

    /** 질문 안에서의 선택지 순서 (1부터 시작) */
    private int orderIndex;

    private String label;

    void assignQuestion(FormQuestionEntity question) {
        this.question = question;
    }
}
