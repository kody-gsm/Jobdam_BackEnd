package com.example.kodyjobdam.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class NotificationExpirationService {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Value("${notification.default-retention-days:90}")
    private long defaultRetentionDays;

    @Value("${notification.recruit-retention-days:30}")
    private long recruitRetentionDays;

    @Value("${notification.form-retention-days:30}")
    private long formRetentionDays;

    @Value("${notification.counseling-retention-days:90}")
    private long counselingRetentionDays;

    @Value("${notification.cleanup.zone:Asia/Seoul}")
    private String zone;

    public LocalDateTime recruitExpiresAt(String deadline) {
        return parseDeadline(deadline)
                .map(date -> date.plusDays(recruitRetentionDays))
                .orElseGet(this::defaultExpiresAt);
    }

    public LocalDateTime formExpiresAt(LocalDateTime deadline) {
        if (deadline == null) {
            return defaultExpiresAt();
        }
        return deadline.plusDays(formRetentionDays);
    }

    public LocalDateTime counselingExpiresAt(LocalDate reservationDate) {
        if (reservationDate == null) {
            return defaultExpiresAt();
        }
        return reservationDate.atStartOfDay().plusDays(counselingRetentionDays);
    }

    public LocalDateTime defaultExpiresAt() {
        return now().plusDays(defaultRetentionDays);
    }

    public boolean isRecruitDeadlineExpired(String deadline) {
        return parseDeadline(deadline)
                .map(date -> !date.isAfter(now()))
                .orElse(false);
    }

    public boolean isFormDeadlineExpired(LocalDateTime deadline) {
        return deadline != null && !deadline.isAfter(now());
    }

    public boolean isCounselingDateExpired(LocalDate reservationDate) {
        return reservationDate != null && reservationDate.atStartOfDay().isBefore(now());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(zone));
    }

    private java.util.Optional<LocalDateTime> parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return java.util.Optional.empty();
        }

        String normalized = deadline.trim();
        try {
            return java.util.Optional.of(LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return java.util.Optional.of(LocalDate.parse(normalized, formatter).atTime(LocalTime.MAX));
            } catch (DateTimeParseException ignored) {
            }
        }

        return java.util.Optional.empty();
    }
}
