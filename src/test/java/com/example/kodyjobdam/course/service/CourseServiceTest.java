package com.example.kodyjobdam.course.service;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.repository.ReservationSlot;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.dto.request.WeeklyLockDTO;
import com.example.kodyjobdam.course.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.course.dto.response.WeeklyLockResponseDTO;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.CourseWeeklyLockEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.course.repository.CourseWeeklyLockRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseWeeklyLockRepository weeklyLockRepository;

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

    @Test
    void lockWeeklyCancelsExistingFutureReservationsOnThatWeekday() {
        User teacher = user(2L, UserRole.TEACHER);
        LocalDate matchingDate = LocalDate.of(2026, 12, 9);
        DayOfWeek dayOfWeek = matchingDate.getDayOfWeek();
        CourseEntity matching = CourseEntity.builder()
                .reservation_id(10L).teacher(teacher).date(matchingDate).period("3교시").state(StateEnum.WAITING)
                .build();
        CourseEntity otherWeekday = CourseEntity.builder()
                .reservation_id(11L).teacher(teacher).date(matchingDate.plusDays(1)).period("3교시")
                .state(StateEnum.WAITING).build();
        CourseWeeklyLockEntity saved = CourseWeeklyLockEntity.builder()
                .id(50L).teacher(teacher).dayOfWeek(dayOfWeek).period("3교시").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(courseRepository.findAllByPeriodAndTeacher_IdAndDateGreaterThanEqual(eq("3교시"), eq(2L), any()))
                .thenReturn(List.of(matching, otherWeekday));
        when(weeklyLockRepository.save(any(CourseWeeklyLockEntity.class))).thenReturn(saved);

        WeeklyLockResponseDTO response = courseService.lockWeekly(weeklyLockDto(dayOfWeek, "3교시"), 2L);

        assertThat(response.dayOfWeek()).isEqualTo(dayOfWeek);
        assertThat(response.period()).isEqualTo("3교시");
        assertThat(matching.getState()).isEqualTo(StateEnum.CANCEL);
        assertThat(otherWeekday.getState()).isEqualTo(StateEnum.WAITING);
    }

    @Test
    void lockWeeklyRejectsDuplicateRule() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.TEACHER)));
        when(weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.lockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("이미 매주 잠가 둔 시간입니다.");

        verify(weeklyLockRepository, never()).save(any());
    }

    @Test
    void unlockWeeklyRemovesRule() {
        CourseWeeklyLockEntity rule = CourseWeeklyLockEntity.builder()
                .id(50L).dayOfWeek(DayOfWeek.WEDNESDAY).period("3교시").build();
        when(weeklyLockRepository.findByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(Optional.of(rule));

        courseService.unlockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L);

        verify(weeklyLockRepository).delete(rule);
    }

    @Test
    void unlockWeeklyWithoutRuleIsRejected() {
        when(weeklyLockRepository.findByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.unlockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("매주 잠근 시간이 아닙니다.");
    }

    @Test
    void createReservationOnWeeklyLockedDayIsRejected() {
        CreateDTO dto = createDto(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.TEACHER)));
        when(weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(2L, dto.getDate().getDayOfWeek(), "3교시"))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("선생님이 매주 잠가 둔 시간입니다.");

        verify(courseRepository, never()).save(any(CourseEntity.class));
    }

    @Test
    void readSlotStatusShowsWeeklyLockForMatchingWeekday() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        User teacher = user(2L, UserRole.TEACHER);
        CourseWeeklyLockEntity rule = CourseWeeklyLockEntity.builder()
                .id(50L).dayOfWeek(date.getDayOfWeek()).period("3교시").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(courseRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L)).thenReturn(List.of());
        when(weeklyLockRepository.findAllByTeacher_Id(2L)).thenReturn(List.of(rule));

        List<SlotStatusDTO> result = courseService.readSlotStatus(2L, date, null, 1L);

        assertThat(result).anySatisfy(slot -> {
            assertThat(slot.getPeriod()).isEqualTo("3교시");
            assertThat(slot.getState()).isEqualTo(StateEnum.LOCKED);
        });
    }

    private WeeklyLockDTO weeklyLockDto(DayOfWeek dayOfWeek, String period) {
        WeeklyLockDTO dto = new WeeklyLockDTO();
        ReflectionTestUtils.setField(dto, "dayOfWeek", dayOfWeek);
        ReflectionTestUtils.setField(dto, "period", period);
        return dto;
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
