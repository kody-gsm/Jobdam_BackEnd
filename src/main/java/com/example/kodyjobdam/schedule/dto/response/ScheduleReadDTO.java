package com.example.kodyjobdam.schedule.dto.response;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ScheduleReadDTO {

    private final LocalDate date;

    private final String name;

    private final String content;

    /** 휴업일 구분명(공휴일, 토요휴업일 등). 수업일이면 null */
    private final String holidayType;

    private final boolean holiday;

    /** 해당 일정이 적용되는 학년 */
    private final List<Integer> grades;

    public ScheduleReadDTO(LocalDate date, String name, String content, String holidayType, List<Integer> grades) {
        this.date = date;
        this.name = name;
        this.content = content;
        this.holidayType = holidayType;
        this.holiday = holidayType != null;
        this.grades = grades;
    }
}
