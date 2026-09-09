package com.example.kodyjobdam.recruit.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
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

    /** 서류 접수 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "document_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "document_end_date"))
    })
    private RecruitPeriod documentPeriod;

    /** 필기 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "written_exam_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "written_exam_end_date"))
    })
    private RecruitPeriod writtenExamPeriod;

    /** 실기 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "practical_exam_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "practical_exam_end_date"))
    })
    private RecruitPeriod practicalExamPeriod;

    /** 코딩테스트 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "coding_test_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "coding_test_end_date"))
    })
    private RecruitPeriod codingTestPeriod;

    /** 면접 전형 기간 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "interview_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "interview_end_date"))
    })
    private RecruitPeriod interviewPeriod;

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
    public void update(String companyName,
                       RecruitPeriod documentPeriod,
                       RecruitPeriod writtenExamPeriod,
                       RecruitPeriod practicalExamPeriod,
                       RecruitPeriod codingTestPeriod,
                       RecruitPeriod interviewPeriod,
                       String summary) {
        this.companyName = companyName;
        this.documentPeriod = documentPeriod;
        this.writtenExamPeriod = writtenExamPeriod;
        this.practicalExamPeriod = practicalExamPeriod;
        this.codingTestPeriod = codingTestPeriod;
        this.interviewPeriod = interviewPeriod;
        this.summary = summary;
    }

    /**
     * 지원 마감일(서류 접수 종료일)을 yyyy-MM-dd 문자열로 돌려준다.
     * 알림 만료 계산이 문자열 마감일을 받으므로 그 형식에 맞춘 파생값이다.
     */
    public String getDeadline() {
        LocalDate endDate = documentPeriod == null ? null : documentPeriod.getEndDate();
        return endDate == null ? null : endDate.toString();
    }

    /** 면접 일정을 화면 표기 문자열로 돌려준다. 하루짜리 면접은 날짜 하나로 표기한다. */
    public String getInterviewDate() {
        return interviewPeriod == null ? null : interviewPeriod.toDisplay();
    }

    /** 학생에게 공개 */
    public void publish() {
        this.status = RecruitStatus.PUBLISHED;
    }
}
