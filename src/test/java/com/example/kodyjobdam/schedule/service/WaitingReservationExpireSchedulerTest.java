package com.example.kodyjobdam.schedule.service;

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
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingReservationExpireSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 0, 5);

    @Mock
    private CommonService commonService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private WaitingReservationExpireScheduler scheduler;

    @BeforeEach
    void setUp() {
        // 한국 시간 00:05는 UTC로 아직 전날이다. 한국 시간으로 판단하는지 함께 확인한다.
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ReflectionTestUtils.setField(scheduler, "clock", Clock.fixed(NOW.atZone(zone).toInstant(), zone));
    }

    @Test
    void 한국_시간_기준으로_일반_진로_상담_대기_신청을_만료시킨다() {
        scheduler.expireWaitingReservations();

        verify(commonService).expireWaitingReservations(NOW);
        verify(courseService).expireWaitingReservations(NOW);
    }

    @Test
    void 일반_상담_만료가_실패해도_진로_상담은_만료시킨다() {
        when(commonService.expireWaitingReservations(NOW)).thenThrow(new IllegalStateException("DB 오류"));

        scheduler.expireWaitingReservations();

        verify(courseService).expireWaitingReservations(NOW);
    }
}
