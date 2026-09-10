package com.example.kodyjobdam.common.service;

import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private CounselingReservationCryptoService cryptoService;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private CommonService commonService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commonService, "lockedPeriods", Set.of("4교시"));
    }

    @Test
    void createReservationNotifiesOnlySelectedTeacher() {
        User student = user(1L, UserRole.STUDENT);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CreateDTO dto = createDto(2L);
        CommonEntity saved = CommonEntity.builder()
                .reservation_id(100L)
                .teacher(teacher)
                .date(dto.getDate())
                .period(dto.getPeriod())
                .category(dto.getCategory())
                .submitterHash("student-hash")
                .encryptedTitle("encrypted-title")
                .encryptedContent("encrypted-content")
                .encryptedUserId("encrypted-user-id")
                .encryptedUserName("encrypted-user-name")
                .encryptedStudentNumber("encrypted-student-number")
                .state(StateEnum.WAITING)
                .build();

        when(commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");
        when(cryptoService.encrypt(anyString())).thenReturn("encrypted");
        when(commonRepository.save(any(CommonEntity.class))).thenReturn(saved);
        when(notificationExpirationService.counselingExpiresAt(dto.getDate()))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.createReservation(dto, 1L);

        ArgumentCaptor<CommonEntity> reservationCaptor = ArgumentCaptor.forClass(CommonEntity.class);
        verify(commonRepository).save(reservationCaptor.capture());
        CommonEntity savedReservation = reservationCaptor.getValue();
        assertThat(savedReservation.getSubmitterHash()).isEqualTo("student-hash");
        assertThat(savedReservation.getCategory()).isEqualTo(CounselingCategoryEnum.EMPLOYMENT);
        assertThat(savedReservation.getEncryptedTitle()).isEqualTo("encrypted");
        assertThat(savedReservation.getEncryptedContent()).isEqualTo("encrypted");
        assertThat(savedReservation.getEncryptedUserId()).isEqualTo("encrypted");
        assertThat(savedReservation.getEncryptedUserName()).isEqualTo("encrypted");
        assertThat(savedReservation.getEncryptedStudentNumber()).isEqualTo("encrypted");
        verify(cryptoService).encrypt("상담");
        verify(cryptoService).encrypt("내용");
        verify(cryptoService).encrypt("1");
        verify(cryptoService).encrypt("사용자1");
        verify(cryptoService).encrypt("3001");

        verify(notificationService).notifyUser(
                eq(teacher),
                eq(NotificationType.COMMON_COUNSELING_REQUESTED),
                eq("새로운 상담 신청"),
                eq("학생이 상담을 신청했습니다."),
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
                .teacher(user(2L, UserRole.WEE_TEACHER))
                .date(LocalDate.of(2026, 9, 10))
                .encryptedUserId("encrypted-user-id")
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
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonEntity reservation = CommonEntity.builder()
                .reservation_id(100L)
                .teacher(teacher)
                .date(LocalDate.of(2026, 9, 10))
                .encryptedUserId("encrypted-user-id")
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(cryptoService.decrypt("encrypted-user-id")).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
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
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonEntity reservation = CommonEntity.builder()
                .reservation_id(100L)
                .teacher(teacher)
                .date(LocalDate.of(2026, 9, 10))
                .encryptedUserId("encrypted-user-id")
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(cryptoService.decrypt("encrypted-user-id")).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
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

    @Test
    void createReservationOnHolidayIsRejected() {
        CreateDTO dto = createDto(2L);
        when(scheduleService.isHoliday(dto.getDate())).thenReturn(true);

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("휴업일에는 상담을 예약할 수 없습니다.");

        verify(commonRepository, never()).save(any(CommonEntity.class));
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void readSlotStatusOnHolidayIsRejected() {
        LocalDate holiday = LocalDate.of(2026, 9, 25);
        when(scheduleService.isHoliday(holiday)).thenReturn(true);

        assertThatThrownBy(() -> commonService.readSlotStatus(2L, holiday, null))
                .isInstanceOf(ReservationException.class)
                .hasMessage("휴업일에는 상담을 예약할 수 없습니다.");
    }

    @Test
    void createReservationIsRejectedWhenScheduleLookupFails() {
        CreateDTO dto = createDto(2L);
        when(scheduleService.isHoliday(dto.getDate()))
                .thenThrow(ScheduleException.badGateway("나이스 학사일정을 불러오지 못했습니다."));

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("학사일정을 확인할 수 없어 예약을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");

        verify(commonRepository, never()).save(any(CommonEntity.class));
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createReservationOnLockedPeriodIsRejected() {
        CreateDTO dto = createDto(2L);
        dto.setPeriod("4교시");

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("4교시에는 상담을 예약할 수 없습니다.");

        verify(commonRepository, never()).save(any(CommonEntity.class));
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void readSlotStatusMarksLockedPeriodEvenWithoutReservation() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L)).thenReturn(List.of());

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, date, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPeriod()).isEqualTo("4교시");
        assertThat(result.get(0).getState()).isEqualTo(StateEnum.LOCKED);
        assertThat(result.get(0).isAvailable()).isFalse();
    }

    @Test
    void teacherUnlockCancelsOnlyLockedSlot() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonEntity locked = CommonEntity.builder().state(StateEnum.LOCKED).build();
        CommonEntity reserved = CommonEntity.builder().state(StateEnum.RESERVED).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.findAllByDateAndPeriodAndTeacher_Id(date, "3교시", 2L))
                .thenReturn(List.of(locked, reserved));

        commonService.teacherUnlock(lockDto(date, "3교시"), 2L);

        assertThat(locked.getState()).isEqualTo(StateEnum.CANCEL);
        assertThat(reserved.getState()).isEqualTo(StateEnum.RESERVED);
    }

    @Test
    void teacherUnlockWithoutLockedSlotIsRejected() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        User teacher = user(2L, UserRole.WEE_TEACHER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.findAllByDateAndPeriodAndTeacher_Id(date, "3교시", 2L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> commonService.teacherUnlock(lockDto(date, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("잠긴 시간이 아닙니다.");
    }

    @Test
    void teacherUnlockOnAlwaysLockedPeriodIsRejected() {
        LocalDate date = LocalDate.of(2026, 9, 10);

        assertThatThrownBy(() -> commonService.teacherUnlock(lockDto(date, "4교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("4교시는 설정으로 상시 잠겨 있어 해제할 수 없습니다.");

        verify(commonRepository, never()).findAllByDateAndPeriodAndTeacher_Id(any(), anyString(), any());
    }

    @Test
    void allowRejectsSlotAlreadyTakenBySameTeacher() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        CommonEntity target = CommonEntity.builder()
                .reservation_id(100L)
                .teacher(user(2L, UserRole.WEE_TEACHER))
                .date(date)
                .period("3교시")
                .state(StateEnum.WAITING)
                .build();
        CommonEntity taken = CommonEntity.builder()
                .reservation_id(101L)
                .date(date)
                .period("3교시")
                .state(StateEnum.RESERVED)
                .build();

        when(commonRepository.findById(100L)).thenReturn(Optional.of(target));
        when(commonRepository.findAllByDateAndPeriodAndTeacher_Id(date, "3교시", 2L))
                .thenReturn(List.of(target, taken));

        assertThatThrownBy(() -> commonService.allow(100L, 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("같은 시간에 이미 수락한 상담이 있습니다.");

        assertThat(target.getState()).isEqualTo(StateEnum.WAITING);
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelWithinOneHourOfStartIsRejected() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        CommonEntity entity = CommonEntity.builder()
                .reservation_id(100L)
                .date(date)
                .period("3교시")                       // 10:40 시작 → 09:40부터 취소 불가
                .submitterHash("student-hash")
                .state(StateEnum.WAITING)
                .build();

        fixClock(LocalDateTime.of(2026, 9, 10, 9, 41));
        when(commonRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");

        assertThatThrownBy(() -> commonService.cancelReservation(100L, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("상담 시작 1시간 전부터는 취소할 수 없습니다.");

        assertThat(entity.getState()).isEqualTo(StateEnum.WAITING);
    }

    @Test
    void cancelBeforeDeadlineIsAllowed() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        CommonEntity entity = CommonEntity.builder()
                .reservation_id(100L)
                .date(date)
                .period("3교시")
                .submitterHash("student-hash")
                .state(StateEnum.WAITING)
                .build();

        fixClock(LocalDateTime.of(2026, 9, 10, 9, 39));
        when(commonRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");

        commonService.cancelReservation(100L, 1L);

        assertThat(entity.getState()).isEqualTo(StateEnum.CANCEL);
    }

    private void fixClock(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        ReflectionTestUtils.setField(commonService, "clock",
                Clock.fixed(now.atZone(zone).toInstant(), zone));
    }

    private LockDTO lockDto(LocalDate date, String period) {
        LockDTO dto = new LockDTO();
        ReflectionTestUtils.setField(dto, "date", date);
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
