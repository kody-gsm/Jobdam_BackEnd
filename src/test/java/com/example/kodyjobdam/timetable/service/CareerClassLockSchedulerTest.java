package com.example.kodyjobdam.timetable.service;

import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerClassLockSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private TimetableService timetableService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CareerClassLockScheduler scheduler;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ReflectionTestUtils.setField(scheduler, "clock",
                Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone));
        ReflectionTestUtils.setField(scheduler, "teacherName", " 임경원 ");
        ReflectionTestUtils.setField(scheduler, "subjects", Set.of("진로활동"));
        ReflectionTestUtils.setField(scheduler, "weeksAhead", 8);
    }

    @Test
    void 오늘부터_설정한_주까지의_진로_수업_교시를_잠근다() {
        Map<LocalDate, Set<String>> classPeriods = Map.of(LocalDate.of(2026, 10, 20), Set.of("2교시"));
        when(timetableService.findSubjectPeriods(TODAY, LocalDate.of(2026, 12, 10), Set.of("진로활동")))
                .thenReturn(classPeriods);

        scheduler.lockCareerClasses();

        verify(courseService).lockClassPeriods("임경원", classPeriods);
    }

    @Test
    void 시간표를_불러오지_못하면_잠그지_않는다() {
        when(timetableService.findSubjectPeriods(any(), any(), any()))
                .thenThrow(ScheduleException.badGateway("나이스 시간표를 불러오지 못했습니다."));

        scheduler.lockCareerClasses();

        verify(courseService, never()).lockClassPeriods(any(), any());
    }

    @Test
    void 설정_파일의_한글_기본값이_깨지지_않고_읽힌다() throws IOException {
        // Spring Boot는 properties 파일을 ISO-8859-1로 읽는다. 한글을 그대로 적으면 이름이 깨져 선생님을 찾지 못한다.
        PropertySource<?> properties = new PropertiesPropertySourceLoader()
                .load("application", new ClassPathResource("application.properties")).get(0);

        assertThat(properties.getProperty("reservation.career-class-lock.teacher-name"))
                .isEqualTo("${CAREER_CLASS_LOCK_TEACHER_NAME:임경원}");
        assertThat(properties.getProperty("reservation.career-class-lock.subjects"))
                .isEqualTo("${CAREER_CLASS_LOCK_SUBJECTS:진로활동}");
    }

    @Test
    void 선생님_이름을_비워_두면_시간표를_읽지_않는다() {
        ReflectionTestUtils.setField(scheduler, "teacherName", "");

        scheduler.lockCareerClasses();

        verifyNoInteractions(timetableService, courseService);
    }
}
