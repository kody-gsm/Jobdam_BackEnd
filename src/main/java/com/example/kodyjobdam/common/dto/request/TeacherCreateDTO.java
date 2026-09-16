package com.example.kodyjobdam.common.dto.request;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class TeacherCreateDTO {

    private Long studentId;

    private String title;

    private String content;

    private CounselingCategoryEnum category;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String period;
}
