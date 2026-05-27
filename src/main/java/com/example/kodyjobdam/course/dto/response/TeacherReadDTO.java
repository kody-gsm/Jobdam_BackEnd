package com.example.kodyjobdam.course.dto.response;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TeacherReadDTO {

    private Long reservation_id;

    private String name;

    private LocalDate date;

    private String period;

    public TeacherReadDTO(Long reservation_id, String name, LocalDate date, String period) {
        this.reservation_id = reservation_id;
        this.name = name;
        this.date = date;
        this.period = period;
    }
}
