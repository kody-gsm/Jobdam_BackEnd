package com.example.kodyjobdam.common.entity;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Builder
@NoArgsConstructor()
@AllArgsConstructor
@Table(name="common")
public class CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservation_id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String period;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    private String content;

    /** 상담을 신청한 대상 선생님의 user id (컬럼명은 기존 allow_id 유지) */
    @Column(name = "allow_id")
    private Long teacherId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING) //DB에 이 ENUM을 문자열로 저장해줘
    private StateEnum state = StateEnum.WAITING;

    /*@Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private KindEnum kind;*/

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
