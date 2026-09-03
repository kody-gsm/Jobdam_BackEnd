package com.example.kodyjobdam.form.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "form")
public class FormEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 폼을 만든 선생님 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;

    @Column(length = 1000)
    private String description;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<FormQuestionEntity> questions = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 제목·설명 수정 */
    public void update(String title, String description, LocalDateTime deadline) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
    }

    /** 질문 추가 (양방향 연관관계 동기화) */
    public void addQuestion(FormQuestionEntity question) {
        question.assignForm(this);
        this.questions.add(question);
    }

    /** 질문 전체 삭제 (수정 시 새 질문으로 교체하기 위함) */
    public void clearQuestions() {
        this.questions.clear();
    }

    /** 학생에게 공개 */
    public void publish() {
        this.status = FormStatus.PUBLISHED;
    }

    /** 응답 마감 */
    public void close() {
        this.status = FormStatus.CLOSED;
    }

    /** 질문 구조를 수정할 수 있는 상태인지 (제출된 응답과 어긋나지 않도록 초안에서만 허용) */
    public boolean isEditable() {
        return this.status == FormStatus.DRAFT;
    }

    /** 응답을 받을 수 있는 상태인지 */
    public boolean isAcceptingSubmission() {
        return this.status == FormStatus.PUBLISHED;
    }
}
