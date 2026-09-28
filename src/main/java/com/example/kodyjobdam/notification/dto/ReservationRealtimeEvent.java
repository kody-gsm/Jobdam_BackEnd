package com.example.kodyjobdam.notification.dto;

import java.time.LocalDate;

public record ReservationRealtimeEvent(
        String counselingType,
        String action,
        Long reservationId,
        LocalDate date,
        String period,
        String status,
        Long teacherId,
        Long studentId
) {
}
