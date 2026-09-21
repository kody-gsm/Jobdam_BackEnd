package com.example.kodyjobdam.common.dto.response;

import com.example.kodyjobdam.common.entity.CommonWeeklyLockEntity;

import java.time.DayOfWeek;

public record WeeklyLockResponseDTO(Long id, DayOfWeek dayOfWeek, String period) {

    public static WeeklyLockResponseDTO from(CommonWeeklyLockEntity entity) {
        return new WeeklyLockResponseDTO(entity.getId(), entity.getDayOfWeek(), entity.getPeriod());
    }
}
