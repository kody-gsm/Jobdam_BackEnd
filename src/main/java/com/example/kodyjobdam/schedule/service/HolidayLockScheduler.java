package com.example.kodyjobdam.schedule.service;

import com.example.kodyjobdam.common.service.CommonService;
import com.example.kodyjobdam.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * 나이스 학사일정의 휴업일을 상담 시간표에 LOCKED 행으로 미리 잠가 둔다.
 * 시간표 조회는 이 행만 보고, 나이스는 부르지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayLockScheduler {

    private final ScheduleService scheduleService;
    private final CommonService commonService;
    private final CourseService courseService;

    /** 오늘이 속한 달부터 몇 달 뒤 말일까지 잠글지 */
    @Value("${reservation.holiday-lock.months-ahead:2}")
    private int monthsAhead;

    /** 학사일정과 같은 한국 시간으로 오늘을 판단한다. */
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    /** 배포 직후에도 휴업일이 잠겨 있도록 기동할 때 한 번 실행한다. */
    @EventListener(ApplicationReadyEvent.class)
    public void lockOnStartup() {
        lockHolidays();
    }

    @Scheduled(cron = "${reservation.holiday-lock.cron:0 10 3 * * *}", zone = "Asia/Seoul")
    public void lockHolidays() {
        LocalDate today = LocalDate.now(clock);
        LocalDate to = YearMonth.from(today).plusMonths(monthsAhead).atEndOfMonth();

        List<LocalDate> holidays;
        try {
            holidays = scheduleService.findHolidays(today, to).stream()
                    .filter(date -> !date.isBefore(today))
                    // 상담 시간표는 평일만 다룬다. 토요휴업일까지 잠그면 쓰지 않는 행만 늘어난다.
                    .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY
                            && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("학사일정을 불러오지 못해 휴업일 잠금을 건너뜁니다. from={}, to={}", today, to, e);
            return;
        }

        lock("일반 상담", () -> commonService.lockHolidays(holidays));
        lock("진로 상담", () -> courseService.lockHolidays(holidays));
    }

    private void lock(String kind, IntSupplier locker) {
        try {
            log.info("{} 휴업일 잠금 {}개 추가", kind, locker.getAsInt());
        } catch (RuntimeException e) {
            log.error("{} 휴업일 잠금 중 오류가 발생했습니다.", kind, e);
        }
    }
}
