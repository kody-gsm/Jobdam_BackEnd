package com.example.kodyjobdam.form.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "form_answer")
public class FormAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private FormSubmissionEntity submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private FormQuestionEntity question;

    /** 주관식·숫자·날짜 답변 (선택형 질문에서는 비어 있음) */
    @Column(columnDefinition = "TEXT")
    private String textValue;

    /** 선택형 질문에서 고른 선택지 (단일 선택은 1개, 다중 선택은 N개) */
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<FormAnswerOptionEntity> selectedOptions = new ArrayList<>();

    void assignSubmission(FormSubmissionEntity submission) {
        this.submission = submission;
    }

    /** 선택한 보기 추가 (양방향 연관관계 동기화) */
    public void addSelectedOption(FormAnswerOptionEntity selectedOption) {
        selectedOption.assignAnswer(this);
        this.selectedOptions.add(selectedOption);
    }
}
