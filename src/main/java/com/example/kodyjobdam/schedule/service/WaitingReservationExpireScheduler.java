package com.example.kodyjobdam.schedule.service;

import com.example.kodyjobdam.common.service.CommonService;
import com.example.kodyjobdam.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.IntSupplier;

/** 선생님이 수락하지 않은 채 날짜가 지난 상담 신청을 매일 취소한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingReservationExpireScheduler {

    private final CommonService commonService;
    private final CourseService courseService;

    /** 상담 날짜와 같은 한국 시간으로 오늘을 판단한다. */
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    /** 자정에 서버가 꺼져 있었어도 밀린 신청이 남지 않도록 기동할 때 한 번 실행한다. */
    @EventListener(ApplicationReadyEvent.class)
    public void expireOnStartup() {
        expireWaitingReservations();
    }

    @Scheduled(cron = "${reservation.waiting-expire.cron:0 5 0 * * *}", zone = "Asia/Seoul")
    public void expireWaitingReservations() {
        LocalDate today = LocalDate.now(clock);
        expire("일반 상담", () -> commonService.expireWaitingReservations(today));
        expire("진로 상담", () -> courseService.expireWaitingReservations(today));
    }

    private void expire(String kind, IntSupplier expirer) {
        try {
            log.info("{} 날짜가 지난 대기 신청 {}개 취소", kind, expirer.getAsInt());
        } catch (RuntimeException e) {
            log.error("{} 대기 신청 만료 처리 중 오류가 발생했습니다.", kind, e);
        }
    }
}
