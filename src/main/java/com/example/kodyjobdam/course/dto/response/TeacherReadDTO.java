package com.example.kodyjobdam.course.dto.response;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TeacherReadDTO {

    private Long reservation_id;

    private String name;

    private String student_number;

    private LocalDate date;

    private String period;

    private CounselingCategoryEnum category;

    private String title;

    private String content;

    public TeacherReadDTO(Long reservation_id, String name, String student_number, LocalDate date, String period,
                          CounselingCategoryEnum category, String title, String content) {
        this.reservation_id = reservation_id;
        this.name = name;
        this.student_number = student_number;
        this.date = date;
        this.period = period;
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
