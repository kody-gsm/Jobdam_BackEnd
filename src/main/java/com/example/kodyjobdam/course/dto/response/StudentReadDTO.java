package com.example.kodyjobdam.course.dto.response;

import com.example.kodyjobdam.common.dto.response.ReservationStatus;
import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudentReadDTO {

    private Long id;

    private String name;

    private Long teacherId;

    private String teacherName;

    private LocalDate date;

    private String period;

    private CounselingCategoryEnum category;

    /** 대기중(WAITING)·확정(RESERVED)·취소(CANCELED)를 구분한다. */
    private ReservationStatus status;

    public StudentReadDTO(Long id, String name, Long teacherId, String teacherName, LocalDate date, String period,
                          CounselingCategoryEnum category, ReservationStatus status) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.date = date;
        this.period = period;
        this.category = category;
        this.status = status;
    }
}
