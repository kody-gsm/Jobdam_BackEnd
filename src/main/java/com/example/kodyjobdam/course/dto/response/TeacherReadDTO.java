package com.example.kodyjobdam.course.dto.response;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TeacherReadDTO {

    private Long reservation_id;

    private String name;

    private LocalDate date;

    private String period;

    private CounselingCategoryEnum category;

    public TeacherReadDTO(Long reservation_id, String name, LocalDate date, String period, CounselingCategoryEnum category) {
        this.reservation_id = reservation_id;
        this.name = name;
        this.date = date;
        this.period = period;
        this.category = category;
    }
}
