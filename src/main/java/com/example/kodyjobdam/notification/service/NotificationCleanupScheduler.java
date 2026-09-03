package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;
    private final NotificationExpirationService notificationExpirationService;

    @Scheduled(
            cron = "${notification.cleanup.cron:0 0 3 * * *}",
            zone = "${notification.cleanup.zone:Asia/Seoul}"
    )
    @Transactional
    public void deleteExpiredNotifications() {
        try {
            int deletedCount = notificationRepository.deleteExpiredNotifications(notificationExpirationService.now());
            log.info("만료된 알림 {}개 삭제 완료", deletedCount);
        } catch (RuntimeException e) {
            log.error("만료된 알림 삭제 중 오류가 발생했습니다.", e);
        }
    }
}
