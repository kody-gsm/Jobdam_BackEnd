package com.example.kodyjobdam.course.dto.response;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudentReadDTO {

    private Long id;

    private String name;

    private LocalDate date;

    private String period;

    private CounselingCategoryEnum category;

    public StudentReadDTO(Long id, String name, LocalDate date, String period, CounselingCategoryEnum category) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.period = period;
        this.category = category;
    }
}
