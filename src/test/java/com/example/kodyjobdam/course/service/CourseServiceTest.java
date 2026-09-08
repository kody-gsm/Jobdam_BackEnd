package com.example.kodyjobdam.course.service;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @Mock
    private CounselingReservationCryptoService cryptoService;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(courseService, "lockedPeriods", Set.of("4"));
    }

    @Test
    void createReservationOnLockedPeriodIsRejected() {
        CreateDTO dto = createDto(2L);
        dto.setPeriod("4");

        assertThatThrownBy(() -> courseService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("4교시에는 상담을 예약할 수 없습니다.");

        verify(courseRepository, never()).save(any(CourseEntity.class));
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void readSlotStatusMarksLockedPeriodEvenWithoutReservation() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        User teacher = user(2L, UserRole.TEACHER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(courseRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L)).thenReturn(List.of());

        List<SlotStatusDTO> result = courseService.readSlotStatus(2L, date, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPeriod()).isEqualTo("4");
        assertThat(result.get(0).getState()).isEqualTo(StateEnum.LOCKED);
        assertThat(result.get(0).isAvailable()).isFalse();
    }

    private CreateDTO createDto(Long teacherId) {
        CreateDTO dto = new CreateDTO();
        dto.setTeacherId(teacherId);
        dto.setTitle("상담");
        dto.setContent("내용");
        dto.setCategory(CounselingCategoryEnum.EMPLOYMENT);
        dto.setDate(LocalDate.of(2026, 9, 10));
        dto.setPeriod("3");
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
