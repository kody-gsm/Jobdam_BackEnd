package com.example.kodyjobdam.course.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course")
public class CourseEntity {

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

    private Long teacher_id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
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

    public void setTeacher_id(Long teacher_id) {
        this.teacher_id = teacher_id;
    }
}
