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
@Table(name = "form_question")
public class FormQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private FormEntity form;

    /** 폼 안에서의 질문 순서 (1부터 시작) */
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_required")
    @Builder.Default
    private boolean required = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<FormQuestionOptionEntity> options = new ArrayList<>();

    void assignForm(FormEntity form) {
        this.form = form;
    }

    /** 선택지 추가 (양방향 연관관계 동기화) */
    public void addOption(FormQuestionOptionEntity option) {
        option.assignQuestion(this);
        this.options.add(option);
    }
}
