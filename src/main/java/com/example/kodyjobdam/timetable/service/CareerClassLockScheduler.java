package com.example.kodyjobdam.timetable.service;

import com.example.kodyjobdam.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * 나이스 시간표에서 진로 수업이 있는 교시를 찾아 진로 선생님의 상담 시간을 LOCKED 행으로 미리 잠가 둔다.
 *
 * <p>나이스는 교사별 시간표를 주지 않으므로, 어느 반이든 진로 과목이 든 교시를 선생님이 수업에 들어가는 시간으로 본다.
 * 시간표는 몇 주 앞까지만 올라오므로 매일 다시 읽어 새로 올라온 주를 잠근다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareerClassLockScheduler {

    private final TimetableService timetableService;
    private final CourseService courseService;

    /** 수업 시간에 상담을 잠글 진로 선생님 이름. 비워 두면 잠그지 않는다. */
    @Value("${reservation.career-class-lock.teacher-name:}")
    private String teacherName;

    /** 진로 선생님이 맡는 수업의 나이스 과목명 */
    @Value("${reservation.career-class-lock.subjects:}")
    private Set<String> subjects;

    /** 오늘부터 몇 주 뒤까지 잠글지 */
    @Value("${reservation.career-class-lock.weeks-ahead:8}")
    private int weeksAhead;

    /** 시간표와 같은 한국 시간으로 오늘을 판단한다. */
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    /** 배포 직후에도 수업 시간이 잠겨 있도록 기동할 때 한 번 실행한다. */
    @EventListener(ApplicationReadyEvent.class)
    public void lockOnStartup() {
        lockCareerClasses();
    }

    @Scheduled(cron = "${reservation.career-class-lock.cron:0 20 3 * * *}", zone = "Asia/Seoul")
    public void lockCareerClasses() {
        if (teacherName == null || teacherName.isBlank() || subjects.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate to = today.plusWeeks(weeksAhead);

        Map<LocalDate, Set<String>> classPeriods;
        try {
            classPeriods = timetableService.findSubjectPeriods(today, to, subjects);
        } catch (RuntimeException e) {
            log.warn("시간표를 불러오지 못해 진로 수업 시간 잠금을 건너뜁니다. from={}, to={}", today, to, e);
            return;
        }

        try {
            log.info("진로 수업 시간 잠금 {}개 추가", courseService.lockClassPeriods(teacherName.trim(), classPeriods));
        } catch (RuntimeException e) {
            log.error("진로 수업 시간 잠금 중 오류가 발생했습니다.", e);
        }
    }
}
