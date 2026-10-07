package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.entity.ReservationKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounselingReminderScheduler {

    private final CounselingReminderService reminderService;
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    @Scheduled(cron = "${discord.counseling-reminder.cron:0 * * * * *}",
            zone = "Asia/Seoul", scheduler = "discordReminderTaskScheduler")
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        send(ReservationKind.COMMON, reminderService.commonReservationIds(now.toLocalDate()));
        send(ReservationKind.COURSE, reminderService.courseReservationIds(now.toLocalDate()));
    }

    private void send(ReservationKind kind, List<Long> ids) {
        for (Long id : ids) {
            for (int hoursBefore : new int[]{2, 1}) {
                try {
                    reminderService.sendIfDue(kind, id, hoursBefore, LocalDateTime.now(clock));
                } catch (RuntimeException e) {
                    log.error("상담 디스코드 알림 발송 실패: 종류={}, 예약={}, {}시간 전",
                            kind, id, hoursBefore, e);
                }
            }
        }
    }
}
