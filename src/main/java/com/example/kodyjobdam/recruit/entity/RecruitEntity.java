package com.example.kodyjobdam.recruit.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recruit")
public class RecruitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String companyName;

    private String interviewDate;

    private String deadline;

    @Column(length = 1000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecruitStatus status = RecruitStatus.DRAFT;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 선생님 검토 후 내용 수정 */
    public void update(String companyName, String interviewDate, String deadline, String summary) {
        this.companyName = companyName;
        this.interviewDate = interviewDate;
        this.deadline = deadline;
        this.summary = summary;
    }

    /** 학생에게 공개 */
    public void publish() {
        this.status = RecruitStatus.PUBLISHED;
    }
}
