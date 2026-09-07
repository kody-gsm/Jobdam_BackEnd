package com.example.kodyjobdam.common.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDate;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "common")
public class CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservation_id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String period;

    @Column(name = "submitter_hash", length = 64)
    private String submitterHash;

    @Column(name = "encrypted_user_id", columnDefinition = "TEXT")
    private String encryptedUserId;

    @Column(name = "encrypted_user_name", columnDefinition = "TEXT")
    private String encryptedUserName;

    @Column(name = "encrypted_student_number", columnDefinition = "TEXT")
    private String encryptedStudentNumber;

    @Column(name = "encrypted_title", columnDefinition = "TEXT")
    private String encryptedTitle;

    @Column(name = "encrypted_content", columnDefinition = "TEXT")
    private String encryptedContent;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private CounselingCategoryEnum category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StateEnum state = StateEnum.WAITING;

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setState(StateEnum state) {
        this.state = state;
    }
}
