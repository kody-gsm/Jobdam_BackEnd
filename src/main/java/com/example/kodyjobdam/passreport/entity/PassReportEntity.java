package com.example.kodyjobdam.passreport.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학생이 진로 선생님들에게 알린 면접 합격 보고.
 *
 * <p>학번·이름은 보고한 순간의 값을 복사해 둔다. 학생 정보가 나중에 바뀌어도 보고 내용은 그대로 남는다.</p>
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pass_report")
public class PassReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 보고한 학생. 학생이 자기 보고를 찾을 때만 쓴다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "student_number", nullable = false, length = 20)
    private String studentNumber;

    @Column(name = "student_name", nullable = false, length = 50)
    private String studentName;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    /**
     * 학생이 보고를 제출한 한국 시각.
     *
     * <p>운영 서버 JVM이 UTC라 {@code @CreationTimestamp}를 쓰면 9시간 늦게 찍힌다. 서비스에서 한국 시간으로 넣는다.</p>
     */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
}
