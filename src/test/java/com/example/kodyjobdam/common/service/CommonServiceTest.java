package com.example.kodyjobdam.common.service;

import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonServiceTest {

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private CommonService commonService;

    @Test
    void createReservationNotifiesOnlySelectedTeacher() {
        User student = user(1L, UserRole.STUDENT);
        User teacher = user(2L, UserRole.TEACHER);
        CreateDTO dto = createDto(2L);
        CommonEntity saved = CommonEntity.builder()
                .reservation_id(100L)
                .user(student)
                .teacher(teacher)
                .date(dto.getDate())
                .period(dto.getPeriod())
                .title(dto.getTitle())
                .content(dto.getContent())
                .state(StateEnum.WAITING)
                .build();

        when(commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.save(any(CommonEntity.class))).thenReturn(saved);
        when(notificationExpirationService.counselingExpiresAt(dto.getDate()))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.createReservation(dto, 1L);

        verify(notificationService).notifyUser(
                eq(teacher),
                eq(NotificationType.COMMON_COUNSELING_REQUESTED),
                eq("새로운 상담 신청"),
                eq("3001 사용자1 학생이 상담을 신청했습니다."),
                eq(100L),
                eq("/teacher/common/100"),
                eq(LocalDateTime.of(2026, 12, 9, 0, 0))
        );
    }

    @Test
    void createReservationRejectsNonTeacher() {
        User student = user(1L, UserRole.STUDENT);
        User notTeacher = user(3L, UserRole.STUDENT);
        CreateDTO dto = createDto(3L);

        when(commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(3L)).thenReturn(Optional.of(notTeacher));

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class);
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void otherTeacherCannotApproveReservation() {
        CommonEntity reservation = CommonEntity.builder()
                .reservation_id(100L)
                .user(user(1L, UserRole.STUDENT))
                .teacher(user(2L, UserRole.TEACHER))
                .date(LocalDate.of(2026, 9, 10))
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> commonService.allow(100L, 3L))
                .isInstanceOf(ReservationException.class);
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveNotifiesStudent() {
        User student = user(1L, UserRole.STUDENT);
        User teacher = user(2L, UserRole.TEACHER);
        CommonEntity reservation = CommonEntity.builder()
                .reservation_id(100L)
                .user(student)
                .teacher(teacher)
                .date(LocalDate.of(2026, 9, 10))
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(notificationExpirationService.counselingExpiresAt(reservation.getDate()))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.allow(100L, 2L);

        verify(notificationService).notifyUser(
                eq(student),
                eq(NotificationType.COUNSELING_APPROVED),
                eq("상담 신청 승인"),
                eq("사용자2 선생님이 상담 신청을 승인했습니다."),
                eq(100L),
                eq("/student/common/100"),
                eq(LocalDateTime.of(2026, 12, 9, 0, 0))
        );
    }

    @Test
    void rejectNotifiesStudent() {
        User student = user(1L, UserRole.STUDENT);
        User teacher = user(2L, UserRole.TEACHER);
        CommonEntity reservation = CommonEntity.builder()
                .reservation_id(100L)
                .user(student)
                .teacher(teacher)
                .date(LocalDate.of(2026, 9, 10))
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(notificationExpirationService.counselingExpiresAt(reservation.getDate()))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.reject(100L, 2L);

        verify(notificationService).notifyUser(
                eq(student),
                eq(NotificationType.COUNSELING_REJECTED),
                eq("상담 신청 거절"),
                eq("사용자2 선생님이 상담 신청을 거절했습니다."),
                eq(100L),
                eq("/student/common/100"),
                eq(LocalDateTime.of(2026, 12, 9, 0, 0))
        );
    }

    private CreateDTO createDto(Long teacherId) {
        CreateDTO dto = new CreateDTO();
        dto.setTeacherId(teacherId);
        dto.setTitle("상담");
        dto.setContent("내용");
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
