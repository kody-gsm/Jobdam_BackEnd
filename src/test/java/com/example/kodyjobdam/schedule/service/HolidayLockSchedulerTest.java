package com.example.kodyjobdam.schedule.service;

import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.common.service.CommonService;
import com.example.kodyjobdam.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayLockSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private CommonService commonService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private HolidayLockScheduler scheduler;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ReflectionTestUtils.setField(scheduler, "clock",
                Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone));
        ReflectionTestUtils.setField(scheduler, "monthsAhead", 2);
    }

    @Test
    void 오늘부터_두_달_뒤_말일까지의_평일_휴업일만_잠근다() {
        LocalDate chuseok = LocalDate.of(2026, 9, 25);      // 금요일
        LocalDate saturday = LocalDate.of(2026, 10, 3);     // 토요일(개천절)
        LocalDate hangul = LocalDate.of(2026, 10, 9);       // 금요일
        when(scheduleService.findHolidays(TODAY, LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(chuseok, saturday, hangul));

        scheduler.lockHolidays();

        verify(commonService).lockHolidays(List.of(chuseok, hangul));
        verify(courseService).lockHolidays(List.of(chuseok, hangul));
    }

    @Test
    void 학사일정을_불러오지_못하면_잠그지_않는다() {
        when(scheduleService.findHolidays(any(), any()))
                .thenThrow(ScheduleException.badGateway("나이스 학사일정을 불러오지 못했습니다."));

        scheduler.lockHolidays();

        verify(commonService, never()).lockHolidays(any());
        verify(courseService, never()).lockHolidays(any());
    }

    @Test
    void 일반_상담_잠금이_실패해도_진로_상담은_잠근다() {
        LocalDate hangul = LocalDate.of(2026, 10, 9);
        when(scheduleService.findHolidays(any(), any())).thenReturn(List.of(hangul));
        when(commonService.lockHolidays(List.of(hangul))).thenThrow(new IllegalStateException("DB 오류"));

        scheduler.lockHolidays();

        verify(courseService).lockHolidays(List.of(hangul));
    }
}
