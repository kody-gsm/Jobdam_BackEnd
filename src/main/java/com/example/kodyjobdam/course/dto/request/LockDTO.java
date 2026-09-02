package com.example.kodyjobdam.course.dto.request;

import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.user.entity.User;

import java.time.LocalDate;

public class LockDTO {

    private LocalDate date;

    private String period;

    private StateEnum state;

    public LocalDate getDate() {
        return date;
    }

    public String getPeriod() {
        return period;
    }

    /*public CourseEntity toEntity2(LockDTO dto) {
        CourseEntity entity = new CourseEntity();
        entity.setPeriod(dto.period);
        entity.setDate(dto.date);
        entity.setState(StateEnum.LOCKED);
        return entity;
    }*/

    public CourseEntity toEntity(LockDTO dto, User teacher) {
        return CourseEntity.builder()
                .date(dto.date)
                .period(dto.period)
                .state(StateEnum.LOCKED)
                .title("Locked")
                .content("이 시간은 잠긴 시간입니다.")
                .user(null)
                .teacher(teacher)
                .build();
    }
}
