package com.example.kodyjobdam.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataGsmStudentSyncScheduler {

    private final DataGsmStudentSyncService studentSyncService;

    @Scheduled(cron = "${datagsm.student-sync.cron:0 0 8 * * SAT}", zone = "Asia/Seoul")
    public void syncStudents() {
        try {
            int syncedCount = studentSyncService.syncAllStudents();
            log.info("DataGSM 학생정보 동기화 완료. count={}", syncedCount);
        } catch (RuntimeException e) {
            log.error("DataGSM 학생정보 동기화 실패", e);
        }
    }
}
