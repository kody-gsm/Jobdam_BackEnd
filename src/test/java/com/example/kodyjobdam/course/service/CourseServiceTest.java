package com.example.kodyjobdam.course.service;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.repository.ReservationSlot;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationExpirationService notificationExpirationService;

    @Mock
    private CounselingReservationCryptoService cryptoService;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createReservationRejectsWhenCommonReservationExistsAtSameTime() {
        CreateDTO dto = createDto(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.TEACHER)));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");
        when(courseRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())).thenReturn(List.of());
        when(commonRepository.existsActiveReservation("student-hash", dto.getDate(), dto.getPeriod()))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("같은 시간에 신청한 일반 상담이 있습니다.");
        verify(courseRepository, never()).save(any());
    }

    @Test
    void allowRejectsSlotAlreadyTakenBySameTeacher() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        CourseEntity target = CourseEntity.builder()
                .reservation_id(100L)
                .teacher(user(2L, UserRole.TEACHER))
                .date(date)
                .period("3교시")
                .state(StateEnum.WAITING)
                .build();
        CourseEntity taken = CourseEntity.builder()
                .reservation_id(101L)
                .date(date)
                .period("3교시")
                .state(StateEnum.RESERVED)
                .build();

        when(courseRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(date, "3교시", 2L)));
        when(courseRepository.findAllForUpdateByDateAndPeriodAndTeacherId(date, "3교시", 2L))
                .thenReturn(List.of(target, taken));

        assertThatThrownBy(() -> courseService.allow(100L, 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("같은 시간에 이미 수락한 상담이 있습니다.");
        assertThat(target.getState()).isEqualTo(StateEnum.WAITING);
    }

    @Test
    void lockHolidaysLocksOnlyCareerTeachers() {
        LocalDate holiday = LocalDate.of(2026, 10, 9);
        User teacher = user(2L, UserRole.TEACHER);
        when(userRepository.findByRole(UserRole.TEACHER)).thenReturn(List.of(teacher));
        when(courseRepository.findAllByDateInAndTeacher_IdIn(List.of(holiday), List.of(2L))).thenReturn(List.of());

        int created = courseService.lockHolidays(List.of(holiday));

        assertThat(created).isEqualTo(CounselingPeriod.values().length);
        verify(userRepository, never()).findByRole(UserRole.WEE_TEACHER);
    }

    private CreateDTO createDto(Long teacherId) {
        CreateDTO dto = new CreateDTO();
        dto.setTeacherId(teacherId);
        dto.setTitle("상담");
        dto.setContent("내용");
        dto.setCategory(CounselingCategoryEnum.EMPLOYMENT);
        dto.setDate(LocalDate.of(2026, 9, 10));
        dto.setPeriod("3교시");
        return dto;
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .name("사용자" + id)
                .student_number("300" + id)
                .email("user" + id + "@test.com")
                .role(role)
                .emailVerified(true)
                .build();
    }
}
