package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.repository.FormFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 올려만 두고 제출하지 않은 첨부 파일을 정리한다.
 * 답변에 붙은 파일은 손대지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormFileCleanupScheduler {

    private final FormFileRepository fileRepository;

    private final FormFileStorage storage;

    @Value("${form.file.orphan-retention-hours:24}")
    private long orphanRetentionHours;

    @Value("${form.file.cleanup.zone:Asia/Seoul}")
    private String zone;

    @Scheduled(
            cron = "${form.file.cleanup.cron:0 30 3 * * *}",
            zone = "${form.file.cleanup.zone:Asia/Seoul}"
    )
    @Transactional
    public void deleteOrphanFiles() {
        try {
            LocalDateTime threshold = LocalDateTime.now(ZoneId.of(zone)).minusHours(orphanRetentionHours);
            List<FormFileEntity> orphans = fileRepository.findOrphans(threshold);
            if (orphans.isEmpty()) {
                return;
            }

            List<String> storedNames = orphans.stream()
                    .map(FormFileEntity::getStoredName)
                    .toList();
            fileRepository.deleteAll(orphans);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        storedNames.forEach(storage::delete);
                    }
                });
            } else {
                storedNames.forEach(storage::delete);
            }
            log.info("제출되지 않은 폼 첨부 파일 {}개 삭제 완료", orphans.size());
        } catch (RuntimeException e) {
            log.error("폼 첨부 파일 정리 중 오류가 발생했습니다.", e);
        }
    }
}
