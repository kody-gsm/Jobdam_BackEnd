package com.example.kodyjobdam.form.entity;

import com.example.kodyjobdam.common.dto.response.PublicationStatus;
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

    /** 제출 기한이 지났는지. 기한이 없는 폼은 지나지 않은 것으로 본다. */
    public boolean isPastDeadline(LocalDateTime now) {
        return this.deadline != null && now.isAfter(this.deadline);
    }

    /**
     * 학생 화면용 공개 상태. 저장된 상태와 제출 기한을 함께 본다.
     *
     * <p>기한이 지나도 저장된 상태는 PUBLISHED로 남으므로 마감 판단은 항상 이 메서드를 거친다.
     * 조회·목록·제출 검사가 같은 기준을 쓰도록 마감 여부를 여기 한 곳에서만 정한다.</p>
     */
    public PublicationStatus publicationStatus(LocalDateTime now) {
        if (this.status == FormStatus.DRAFT) {
            return PublicationStatus.DRAFT;
        }
        if (this.status == FormStatus.CLOSED || isPastDeadline(now)) {
            return PublicationStatus.CLOSED;
        }
        return PublicationStatus.PUBLISHED;
    }
}
