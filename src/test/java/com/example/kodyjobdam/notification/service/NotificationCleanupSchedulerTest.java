package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCleanupSchedulerTest {

    @Test
    void schedulerUsesBulkDeleteQuery() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationExpirationService expirationService = mock(NotificationExpirationService.class);
        NotificationCleanupScheduler scheduler = new NotificationCleanupScheduler(repository, expirationService);
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 3, 0);
        when(expirationService.now()).thenReturn(now);

        scheduler.deleteExpiredNotifications();

        verify(repository).deleteExpiredNotifications(now);
    }

    @Test
    void schedulerFailureDoesNotPropagate() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationExpirationService expirationService = mock(NotificationExpirationService.class);
        NotificationCleanupScheduler scheduler = new NotificationCleanupScheduler(repository, expirationService);
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 3, 0);
        when(expirationService.now()).thenReturn(now);
        doThrow(new RuntimeException("database error")).when(repository).deleteExpiredNotifications(now);

        scheduler.deleteExpiredNotifications();

        verify(repository).deleteExpiredNotifications(now);
    }
}
