package com.example.kodyjobdam.course.dto.response;

import com.example.kodyjobdam.course.entity.CourseWeeklyLockEntity;

import java.time.DayOfWeek;

public record WeeklyLockResponseDTO(Long id, DayOfWeek dayOfWeek, String period) {

    public static WeeklyLockResponseDTO from(CourseWeeklyLockEntity entity) {
        return new WeeklyLockResponseDTO(entity.getId(), entity.getDayOfWeek(), entity.getPeriod());
    }
}
