package com.example.kodyjobdam.common.service;

import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.request.WeeklyLockDTO;
import com.example.kodyjobdam.common.dto.response.ReservationStatus;
import com.example.kodyjobdam.common.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.common.dto.response.StudentReadDTO;
import com.example.kodyjobdam.common.dto.response.WeeklyLockResponseDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.CommonWeeklyLockEntity;
import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.repository.CommonWeeklyLockRepository;
import com.example.kodyjobdam.common.repository.ReservationSlot;
import com.example.kodyjobdam.course.repository.CourseRepository;
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
import java.time.DayOfWeek;
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
    private CommonWeeklyLockRepository weeklyLockRepository;

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
        when(commonRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(reservation.getDate(), "3교시", 2L)));

        assertThatThrownBy(() -> commonService.allow(100L, 3L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("담당 선생님만 처리할 수 있습니다.");
        verify(commonRepository, never()).findAllForUpdateByDateAndPeriodAndTeacherId(any(), anyString(), any());
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
                .period("3교시")
                .state(StateEnum.WAITING)
                .build();
        when(commonRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(reservation.getDate(), "3교시", 2L)));
        when(commonRepository.findAllForUpdateByDateAndPeriodAndTeacherId(reservation.getDate(), "3교시", 2L))
                .thenReturn(List.of(reservation));
        when(cryptoService.decrypt("encrypted-user-id")).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(notificationExpirationService.counselingExpiresAt(reservation.getDate()))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.allow(100L, 2L);

        assertThat(reservation.getState()).isEqualTo(StateEnum.RESERVED);

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
    void approveCancelsOtherWaitingReservationsAndNotifiesThem() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        User otherStudent = user(3L, UserRole.STUDENT);
        CommonEntity accepted = CommonEntity.builder()
                .reservation_id(100L).teacher(teacher).date(date).period("3교시")
                .encryptedUserId("encrypted-user-id-1").state(StateEnum.WAITING).build();
        CommonEntity otherWaiting = CommonEntity.builder()
                .reservation_id(101L).teacher(teacher).date(date).period("3교시")
                .encryptedUserId("encrypted-user-id-3").state(StateEnum.WAITING).build();
        CommonEntity alreadyCanceled = CommonEntity.builder()
                .reservation_id(102L).teacher(teacher).date(date).period("3교시")
                .encryptedUserId("encrypted-user-id-3").state(StateEnum.CANCEL).build();

        when(commonRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(date, "3교시", 2L)));
        when(commonRepository.findAllForUpdateByDateAndPeriodAndTeacherId(date, "3교시", 2L))
                .thenReturn(List.of(accepted, otherWaiting, alreadyCanceled));
        when(cryptoService.decrypt("encrypted-user-id-1")).thenReturn("1");
        when(cryptoService.decrypt("encrypted-user-id-3")).thenReturn("3");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherStudent));
        when(notificationExpirationService.counselingExpiresAt(date))
                .thenReturn(LocalDateTime.of(2026, 12, 9, 0, 0));

        commonService.allow(100L, 2L);

        assertThat(accepted.getState()).isEqualTo(StateEnum.RESERVED);
        assertThat(otherWaiting.getState()).isEqualTo(StateEnum.CANCEL);
        assertThat(alreadyCanceled.getState()).isEqualTo(StateEnum.CANCEL);

        verify(notificationService).notifyUser(
                eq(otherStudent),
                eq(NotificationType.COUNSELING_AUTO_CANCELED),
                eq("상담 신청 자동 취소"),
                eq("사용자2 선생님이 같은 시간에 다른 학생의 상담을 수락하여 신청이 취소되었습니다."),
                eq(101L),
                eq("/student/common/101"),
                eq(LocalDateTime.of(2026, 12, 9, 0, 0))
        );
        // 원래도 취소 상태였던 신청까지 다시 알리지는 않는다.
        verify(notificationService, never()).notifyUser(
                any(), eq(NotificationType.COUNSELING_AUTO_CANCELED), any(), any(), eq(102L), any(), any());
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
    void readSlotStatusShowsHolidayLockWithoutCallingSchedule() {
        LocalDate holiday = LocalDate.of(2026, 9, 25);
        CommonEntity holidayLock = CommonEntity.builder()
                .reservation_id(100L)
                .date(holiday)
                .period("1교시")
                .state(StateEnum.LOCKED)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(holiday, 2L)).thenReturn(List.of(holidayLock));

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, holiday, null, 1L);

        assertThat(result).extracting(SlotStatusDTO::getPeriod).containsExactly("1교시", "4교시");
        assertThat(result).allMatch(slot -> slot.getState() == StateEnum.LOCKED);
        verify(scheduleService, never()).isHoliday(any());
    }

    @Test
    void lockHolidaysLocksEveryPeriodForEachWeeTeacher() {
        LocalDate holiday = LocalDate.of(2026, 10, 9);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonEntity alreadyLocked = CommonEntity.builder()
                .reservation_id(100L).teacher(teacher).date(holiday).period("1교시").state(StateEnum.LOCKED).build();
        CommonEntity waiting = CommonEntity.builder()
                .reservation_id(101L).teacher(teacher).date(holiday).period("2교시").state(StateEnum.WAITING).build();

        when(userRepository.findByRole(UserRole.WEE_TEACHER)).thenReturn(List.of(teacher));
        when(commonRepository.findAllByDateInAndTeacher_IdIn(List.of(holiday), List.of(2L)))
                .thenReturn(List.of(alreadyLocked, waiting));

        int created = commonService.lockHolidays(List.of(holiday));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CommonEntity>> locksCaptor = ArgumentCaptor.forClass(List.class);
        verify(commonRepository).saveAll(locksCaptor.capture());
        List<CommonEntity> locks = locksCaptor.getValue();

        assertThat(created).isEqualTo(CounselingPeriod.values().length - 1);
        assertThat(locks).hasSize(created);
        assertThat(locks).extracting(CommonEntity::getPeriod).doesNotContain("1교시").contains("2교시", "점심시간");
        assertThat(locks).allMatch(lock -> lock.getState() == StateEnum.LOCKED
                && lock.getTeacher() == teacher
                && lock.getDate().equals(holiday));
        assertThat(waiting.getState()).isEqualTo(StateEnum.CANCEL);
    }

    @Test
    void teacherUnlockOnHolidayIsRejected() {
        LocalDate holiday = LocalDate.of(2026, 10, 9);
        when(scheduleService.isHoliday(holiday)).thenReturn(true);

        assertThatThrownBy(() -> commonService.teacherUnlock(lockDto(holiday, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("휴업일은 잠금을 해제할 수 없습니다.");
        verify(commonRepository, never()).findAllByDateAndPeriodAndTeacher_Id(any(), anyString(), any());
    }

    @Test
    void teacherUnlockProceedsWhenScheduleLookupFails() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        CommonEntity locked = CommonEntity.builder().state(StateEnum.LOCKED).build();
        when(scheduleService.isHoliday(date))
                .thenThrow(ScheduleException.badGateway("나이스 학사일정을 불러오지 못했습니다."));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(commonRepository.findAllByDateAndPeriodAndTeacher_Id(date, "3교시", 2L)).thenReturn(List.of(locked));

        commonService.teacherUnlock(lockDto(date, "3교시"), 2L);

        assertThat(locked.getState()).isEqualTo(StateEnum.CANCEL);
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

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, date, null, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPeriod()).isEqualTo("4교시");
        assertThat(result.get(0).getState()).isEqualTo(StateEnum.LOCKED);
        assertThat(result.get(0).isAvailable()).isFalse();
    }

    @Test
    void readSlotStatusMarksPeriodsTheStudentAlreadyRequested() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L)).thenReturn(List.of());
        when(cryptoService.submitterHash(1L)).thenReturn("hash");
        when(commonRepository.findActivePeriods("hash", date)).thenReturn(List.of("1교시"));
        when(courseRepository.findActivePeriods("hash", date)).thenReturn(List.of("2교시"));

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, date, null, 1L);

        assertThat(result).extracting(SlotStatusDTO::getPeriod).containsExactly("1교시", "2교시", "4교시");
        // 다른 선생님에게 신청했어도 같은 시간은 다시 신청할 수 없다.
        assertThat(result.get(0).isMine()).isTrue();
        assertThat(result.get(0).getState()).isNull();
        assertThat(result.get(0).isAvailable()).isFalse();
        // 진로 상담으로 잡아 둔 시간도 막는다.
        assertThat(result.get(1).isMine()).isTrue();
        assertThat(result.get(1).isAvailable()).isFalse();
        assertThat(result.get(2).getState()).isEqualTo(StateEnum.LOCKED);
        assertThat(result.get(2).isMine()).isFalse();
    }

    @Test
    void readSlotStatusKeepsOtherStudentsWaitingAvailable() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        CommonEntity othersWaiting = CommonEntity.builder()
                .reservation_id(100L)
                .date(date)
                .period("1교시")
                .state(StateEnum.WAITING)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L))
                .thenReturn(List.of(othersWaiting));
        when(cryptoService.submitterHash(1L)).thenReturn("hash");

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, date, null, 1L);

        // 선생님이 한 명을 고르는 구조라 남의 신청은 자리를 막지 않는다.
        assertThat(result.get(0).getPeriod()).isEqualTo("1교시");
        assertThat(result.get(0).getState()).isEqualTo(StateEnum.WAITING);
        assertThat(result.get(0).isMine()).isFalse();
        assertThat(result.get(0).isAvailable()).isTrue();
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

        when(commonRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(date, "3교시", 2L)));
        when(commonRepository.findAllForUpdateByDateAndPeriodAndTeacherId(date, "3교시", 2L))
                .thenReturn(List.of(target, taken));

        assertThatThrownBy(() -> commonService.allow(100L, 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("같은 시간에 이미 수락한 상담이 있습니다.");

        assertThat(target.getState()).isEqualTo(StateEnum.WAITING);
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void allowRejectsReservationAlreadyHandledWhileWaitingForLock() {
        // 같은 예약을 먼저 수락한 요청이 커밋한 뒤, 잠금 조회는 바뀐 상태를 읽는다.
        LocalDate date = LocalDate.of(2026, 9, 10);
        CommonEntity alreadyReserved = CommonEntity.builder()
                .reservation_id(100L)
                .teacher(user(2L, UserRole.WEE_TEACHER))
                .date(date)
                .period("3교시")
                .state(StateEnum.RESERVED)
                .build();

        when(commonRepository.findSlotByReservationId(100L))
                .thenReturn(Optional.of(new ReservationSlot(date, "3교시", 2L)));
        when(commonRepository.findAllForUpdateByDateAndPeriodAndTeacherId(date, "3교시", 2L))
                .thenReturn(List.of(alreadyReserved));

        assertThatThrownBy(() -> commonService.allow(100L, 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("이미 처리된 예약입니다.");
        verify(commonRepository, never()).findById(any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createReservationRejectsWhenCourseReservationExistsAtSameTime() {
        CreateDTO dto = createDto(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");
        when(commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())).thenReturn(List.of());
        when(courseRepository.existsActiveReservation("student-hash", dto.getDate(), dto.getPeriod()))
                .thenReturn(true);

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("같은 시간에 신청한 진로 상담이 있습니다.");
        verify(commonRepository, never()).save(any());
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

    @Test
    void studentReadReturnsStatusInFrontendFormat() {
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonEntity waiting = CommonEntity.builder()
                .reservation_id(1L).date(LocalDate.of(2026, 9, 10)).period("1교시")
                .submitterHash("student-hash").teacher(teacher).state(StateEnum.WAITING).build();
        CommonEntity reserved = CommonEntity.builder()
                .reservation_id(2L).date(LocalDate.of(2026, 9, 10)).period("2교시")
                .submitterHash("student-hash").teacher(teacher).state(StateEnum.RESERVED).build();
        CommonEntity canceled = CommonEntity.builder()
                .reservation_id(3L).date(LocalDate.of(2026, 9, 10)).period("3교시")
                .submitterHash("student-hash").teacher(teacher).state(StateEnum.CANCEL).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(cryptoService.submitterHash(1L)).thenReturn("student-hash");
        when(commonRepository.findBySubmitterHash("student-hash")).thenReturn(List.of(waiting, reserved, canceled));

        List<StudentReadDTO> result = commonService.S_Read(1L);

        assertThat(result).extracting(StudentReadDTO::getStatus)
                .containsExactly(ReservationStatus.WAITING, ReservationStatus.RESERVED, ReservationStatus.CANCELED);
        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getTeacherId()).isEqualTo(2L);
            assertThat(dto.getTeacherName()).isEqualTo("사용자2");
        });
    }

    @Test
    void lockWeeklyCancelsExistingFutureReservationsOnThatWeekday() {
        User teacher = user(2L, UserRole.WEE_TEACHER);
        LocalDate matchingDate = LocalDate.of(2026, 12, 9);
        DayOfWeek dayOfWeek = matchingDate.getDayOfWeek();
        CommonEntity matching = CommonEntity.builder()
                .reservation_id(10L).teacher(teacher).date(matchingDate).period("3교시").state(StateEnum.WAITING)
                .build();
        CommonEntity otherWeekday = CommonEntity.builder()
                .reservation_id(11L).teacher(teacher).date(matchingDate.plusDays(1)).period("3교시")
                .state(StateEnum.WAITING).build();
        CommonWeeklyLockEntity saved = CommonWeeklyLockEntity.builder()
                .id(50L).teacher(teacher).dayOfWeek(dayOfWeek).period("3교시").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.findAllByPeriodAndTeacher_IdAndDateGreaterThanEqual(eq("3교시"), eq(2L), any()))
                .thenReturn(List.of(matching, otherWeekday));
        when(weeklyLockRepository.save(any(CommonWeeklyLockEntity.class))).thenReturn(saved);

        WeeklyLockResponseDTO response = commonService.lockWeekly(weeklyLockDto(dayOfWeek, "3교시"), 2L);

        assertThat(response.dayOfWeek()).isEqualTo(dayOfWeek);
        assertThat(response.period()).isEqualTo("3교시");
        assertThat(matching.getState()).isEqualTo(StateEnum.CANCEL);
        assertThat(otherWeekday.getState()).isEqualTo(StateEnum.WAITING);
    }

    @Test
    void lockWeeklyRejectsDuplicateRule() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(true);

        assertThatThrownBy(() -> commonService.lockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("이미 매주 잠가 둔 시간입니다.");

        verify(weeklyLockRepository, never()).save(any());
    }

    @Test
    void unlockWeeklyRemovesRule() {
        CommonWeeklyLockEntity rule = CommonWeeklyLockEntity.builder()
                .id(50L).dayOfWeek(DayOfWeek.WEDNESDAY).period("3교시").build();
        when(weeklyLockRepository.findByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(Optional.of(rule));

        commonService.unlockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L);

        verify(weeklyLockRepository).delete(rule);
    }

    @Test
    void unlockWeeklyWithoutRuleIsRejected() {
        when(weeklyLockRepository.findByTeacher_IdAndDayOfWeekAndPeriod(2L, DayOfWeek.WEDNESDAY, "3교시"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commonService.unlockWeekly(weeklyLockDto(DayOfWeek.WEDNESDAY, "3교시"), 2L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("매주 잠근 시간이 아닙니다.");
    }

    @Test
    void createReservationOnWeeklyLockedDayIsRejected() {
        CreateDTO dto = createDto(2L);
        dto.setPeriod("3교시");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.STUDENT)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.WEE_TEACHER)));
        when(weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(2L, dto.getDate().getDayOfWeek(), "3교시"))
                .thenReturn(true);

        assertThatThrownBy(() -> commonService.createReservation(dto, 1L))
                .isInstanceOf(ReservationException.class)
                .hasMessage("선생님이 매주 잠가 둔 시간입니다.");

        verify(commonRepository, never()).save(any(CommonEntity.class));
    }

    @Test
    void readSlotStatusShowsWeeklyLockForMatchingWeekday() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        User teacher = user(2L, UserRole.WEE_TEACHER);
        CommonWeeklyLockEntity rule = CommonWeeklyLockEntity.builder()
                .id(50L).dayOfWeek(date.getDayOfWeek()).period("3교시").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, 2L)).thenReturn(List.of());
        when(weeklyLockRepository.findAllByTeacher_Id(2L)).thenReturn(List.of(rule));

        List<SlotStatusDTO> result = commonService.readSlotStatus(2L, date, null, 1L);

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
