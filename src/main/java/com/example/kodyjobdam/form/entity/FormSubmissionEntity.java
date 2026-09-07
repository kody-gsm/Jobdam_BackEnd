package com.example.kodyjobdam.form.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "form_submission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_form_submission_form_user",
                columnNames = {"form_id", "user_id"}
        )
)
public class FormSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private FormEntity form;

    /** 제출한 학생 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<FormAnswerEntity> answers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    /** 확정 시각. 확정하지 않았거나 확정을 되돌리면 null */
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    /** 선생님이 지원자로 확정 */
    public void confirm() {
        this.status = SubmissionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /** 확정 되돌리기 */
    public void cancelConfirm() {
        this.status = SubmissionStatus.SUBMITTED;
        this.confirmedAt = null;
    }

    /** 답변 추가 (양방향 연관관계 동기화) */
    public void addAnswer(FormAnswerEntity answer) {
        answer.assignSubmission(this);
        this.answers.add(answer);
    }
}
