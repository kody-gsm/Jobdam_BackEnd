package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.notification.dto.ReservationRealtimeEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationRealtimeLoadTest {

    @Test
    void reservationRealtimeFanOutHandlesBurstUpdates() {
        NotificationSseService sseService = new NotificationSseService();
        ReservationRealtimeService realtimeService = new ReservationRealtimeService(sseService);

        int students = 1_000;
        int updatesPerStudent = 10;
        long teacherId = 99_999L;

        sseService.subscribe(teacherId);
        for (long studentId = 1; studentId <= students; studentId++) {
            sseService.subscribe(studentId);
        }

        long startedAt = System.nanoTime();
        for (int round = 0; round < updatesPerStudent; round++) {
            for (long studentId = 1; studentId <= students; studentId++) {
                ReservationRealtimeEvent event = new ReservationRealtimeEvent(
                        "COMMON",
                        "REQUESTED",
                        round * (long) students + studentId,
                        LocalDate.of(2026, 12, 10),
                        "3교시",
                        "WAITING",
                        teacherId,
                        studentId
                );

                realtimeService.sendAfterCommit(Arrays.asList(studentId, teacherId), event);
            }
        }

        long elapsedNanos = System.nanoTime() - startedAt;
        long receiverEvents = students * (long) updatesPerStudent * 2L;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        double eventsPerSecond = receiverEvents / (elapsedNanos / 1_000_000_000.0);

        System.out.printf(Locale.ROOT,
                "Reservation SSE load result: connectedUsers=%d updates=%d receiverEvents=%d elapsedMs=%.2f eventsPerSecond=%.2f%n",
                students + 1,
                students * updatesPerStudent,
                receiverEvents,
                elapsedMillis,
                eventsPerSecond);

        assertThat(receiverEvents).isEqualTo(20_000L);
        assertThat(elapsedMillis).isLessThan(5_000.0);
    }
}
