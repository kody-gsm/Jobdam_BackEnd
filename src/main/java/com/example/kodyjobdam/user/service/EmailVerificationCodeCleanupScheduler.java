package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailVerificationCodeCleanupScheduler {

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Value("${app.email-verification.retention-days:1}")
    private long retentionDays;

    @Scheduled(cron = "${app.email-verification.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredOrUsedCodes() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        emailVerificationCodeRepository.deleteExpiredOrUsedBefore(threshold);
    }
}
