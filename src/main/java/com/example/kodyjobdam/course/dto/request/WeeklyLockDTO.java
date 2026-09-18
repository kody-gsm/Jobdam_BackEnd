package com.example.kodyjobdam.course.dto.request;

import java.time.DayOfWeek;

public class WeeklyLockDTO {

    private DayOfWeek dayOfWeek;

    private String period;

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public String getPeriod() {
        return period;
    }
}
