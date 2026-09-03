package com.example.kodyjobdam.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationExpirationServiceTest {

    private NotificationExpirationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationExpirationService();
        ReflectionTestUtils.setField(service, "defaultRetentionDays", 90L);
        ReflectionTestUtils.setField(service, "recruitRetentionDays", 30L);
        ReflectionTestUtils.setField(service, "formRetentionDays", 30L);
        ReflectionTestUtils.setField(service, "counselingRetentionDays", 90L);
        ReflectionTestUtils.setField(service, "zone", "Asia/Seoul");
    }

    @Test
    void recruitNotificationExpiresThirtyDaysAfterDeadline() {
        assertThat(service.recruitExpiresAt("2026-09-10"))
                .isEqualTo(LocalDateTime.of(2026, 10, 10, 23, 59, 59, 999999999));
    }

    @Test
    void formNotificationExpiresThirtyDaysAfterDeadline() {
        LocalDateTime deadline = LocalDateTime.of(2026, 9, 10, 18, 0);

        assertThat(service.formExpiresAt(deadline))
                .isEqualTo(LocalDateTime.of(2026, 10, 10, 18, 0));
    }

    @Test
    void counselingNotificationExpiresNinetyDaysAfterReservationDate() {
        assertThat(service.counselingExpiresAt(LocalDate.of(2026, 9, 10)))
                .isEqualTo(LocalDateTime.of(2026, 12, 9, 0, 0));
    }

    @Test
    void defaultExpiresAtUsesNinetyDaysWhenBaseDateIsMissing() {
        LocalDateTime before = service.now().plusDays(90).minusSeconds(1);

        LocalDateTime expiresAt = service.recruitExpiresAt(null);

        LocalDateTime after = service.now().plusDays(90).plusSeconds(1);
        assertThat(expiresAt).isBetween(before, after);
    }

    @Test
    void seoulTimezoneKeepsDateDeadlineUntilEndOfDay() {
        LocalDateTime expiresAt = service.recruitExpiresAt("2026-09-10");

        assertThat(expiresAt.toLocalTime()).isEqualTo(java.time.LocalTime.MAX);
    }
}
