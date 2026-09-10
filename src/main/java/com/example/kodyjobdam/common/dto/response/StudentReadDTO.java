package com.example.kodyjobdam.common.dto.response;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.StateEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudentReadDTO {

    private Long id;

    private String name;

    private LocalDate date;

    private String period;

    private CounselingCategoryEnum category;

    /** 대기중(WAITING)·확정(RESERVED)·취소(CANCEL)를 구분한다. */
    private StateEnum state;

    public StudentReadDTO(Long id, String name, LocalDate date, String period, CounselingCategoryEnum category,
                          StateEnum state) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.period = period;
        this.category = category;
        this.state = state;
    }
}
