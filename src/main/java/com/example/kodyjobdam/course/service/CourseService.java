package com.example.kodyjobdam.course.service;

import com.example.kodyjobdam.common.dto.response.ReservationStatus;
import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.exception.BusinessException;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.repository.ReservationSlot;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.dto.request.LockDTO;
import com.example.kodyjobdam.course.dto.request.TeacherCreateDTO;
import com.example.kodyjobdam.course.dto.request.WeeklyLockDTO;
import com.example.kodyjobdam.course.dto.response.StudentReadDTO;
import com.example.kodyjobdam.course.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.course.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.course.dto.response.WeeklyLockResponseDTO;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.CourseWeeklyLockEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.course.repository.CourseWeeklyLockRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.dto.ReservationRealtimeEvent;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.notification.service.ReservationRealtimeService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseService {

    /** 상담 시작 이 시간 전부터는 학생이 취소할 수 없다. */
    private static final Duration CANCEL_DEADLINE = Duration.ofHours(1);

    /** 상담 시작 이 시간 전부터는 학생이 신청할 수 없다. */
    private static final Duration APPLY_DEADLINE = Duration.ofMinutes(30);

    private final CourseRepository courseRepository;
    private final CourseWeeklyLockRepository weeklyLockRepository;
    private final CommonRepository commonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationExpirationService notificationExpirationService;
    private final ReservationRealtimeService reservationRealtimeService;
    private final CounselingReservationCryptoService cryptoService;
    private final ScheduleService scheduleService;

    /** 취소 마감을 판단하는 기준 시계. 학사 일정과 같은 한국 시간으로 본다. */
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    public void courseSave(CourseEntity entity) {
        courseRepository.save(entity);
    }

    @Transactional
    public void createReservation(CreateDTO dto, Long id) {
        String period = validateReservationSlot(dto.getDate(), dto.getPeriod());
        validateBeforeApplyDeadline(dto.getDate(), period);
        dto.setPeriod(period);
        validateNotHoliday(dto.getDate());

        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        User teacher = findTeacher(dto.getTeacherId(), id);
        validateCategory(dto.getCategory());
        validateNotWeeklyLocked(dto.getDate(), period, teacher.getId());
        String submitterHash = cryptoService.submitterHash(id);

        for (CourseEntity entity : courseRepository.findAllByDateAndPeriod(dto.getDate(), period)) {
            if (submitterHash.equals(entity.getSubmitterHash())
                    && entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
        }
        if (commonRepository.existsActiveReservation(submitterHash, dto.getDate(), period)) {
            throw ReservationException.conflict("같은 시간에 신청한 일반 상담이 있습니다.");
        }

        for (CourseEntity entity : courseRepository.findAllByDateAndPeriodAndTeacher_Id(
                dto.getDate(), period, teacher.getId())) {
            if (entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {
                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        CourseEntity reservation = courseRepository.save(dto.toEntity(
                teacher,
                submitterHash,
                cryptoService.encrypt(dto.getTitle()),
                cryptoService.encrypt(dto.getContent()),
                cryptoService.encrypt(String.valueOf(user.getId())),
                cryptoService.encrypt(user.getName()),
                cryptoService.encrypt(user.getStudent_number())
        ));
        notificationService.notifyUser(
                teacher,
                NotificationType.COURSE_COUNSELING_REQUESTED,
                "새로운 상담 신청",
                "학생이 상담을 신청했습니다.",
                reservation.getReservation_id(),
                "/teacher/course/" + reservation.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(reservation.getDate())
        );
        publishReservationChange(reservation, user.getId(), "REQUESTED");
    }

    @Transactional
    public void createReservationByTeacher(TeacherCreateDTO dto, Long teacherId) {
        String period = validateReservationSlot(dto.getDate(), dto.getPeriod());
        dto.setPeriod(period);
        validateNotHoliday(dto.getDate());

        User teacher = findTeacher(teacherId);
        User student = findStudentForUpdate(dto.getStudentId());
        validateCategory(dto.getCategory());
        validateNotWeeklyLocked(dto.getDate(), period, teacher.getId());
        String submitterHash = cryptoService.submitterHash(student.getId());

        for (CourseEntity entity : courseRepository.findAllByDateAndPeriod(dto.getDate(), period)) {
            if (submitterHash.equals(entity.getSubmitterHash())
                    && entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
        }
        if (commonRepository.existsActiveReservation(submitterHash, dto.getDate(), period)) {
            throw ReservationException.conflict("같은 시간에 신청한 일반 상담이 있습니다.");
        }

        // 같은 시간 예약을 잠근 뒤 상태를 봐야 학생 신청 수락과 동시에 들어와도 둘 다 통과하지 않는다.
        List<CourseEntity> slotReservations = courseRepository.findAllForUpdateByDateAndPeriodAndTeacherId(
                dto.getDate(), period, teacher.getId());
        for (CourseEntity entity : slotReservations) {
            if (entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {
                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        CourseEntity reservation = courseRepository.save(CourseEntity.builder()
                .teacher(teacher)
                .submitterHash(submitterHash)
                .encryptedTitle(cryptoService.encrypt(dto.getTitle()))
                .encryptedContent(cryptoService.encrypt(dto.getContent()))
                .category(dto.getCategory())
                .encryptedUserId(cryptoService.encrypt(String.valueOf(student.getId())))
                .encryptedUserName(cryptoService.encrypt(student.getName()))
                .encryptedStudentNumber(cryptoService.encrypt(student.getStudent_number()))
                .date(dto.getDate())
                .period(period)
                .state(StateEnum.RESERVED)
                .build());
        notificationService.notifyUser(
                student,
                NotificationType.COUNSELING_APPROVED,
                "상담 일정 등록",
                teacher.getName() + " 선생님이 상담 일정을 등록했습니다.",
                reservation.getReservation_id(),
                "/student/course/" + reservation.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(reservation.getDate())
        );
        publishReservationChange(reservation, student.getId(), "CREATED_BY_TEACHER");
        cancelOtherWaitingReservations(reservation, slotReservations);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("취소 할 수 없습니다."));

        if (!cryptoService.submitterHash(userId).equals(entity.getSubmitterHash())) {
            throw ReservationException.forbidden("권한이 없습니다.");
        }
        validateCancelDeadline(entity.getDate(), entity.getPeriod());

        entity.setState(StateEnum.CANCEL);
        publishReservationChange(entity, userId, "CANCELED_BY_STUDENT");
    }

    @Transactional
    public void allow(Long reservationId, Long teacherId) {
        ReservationSlot slot = courseRepository.findSlotByReservationId(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));
        if (!slot.teacherId().equals(teacherId)) {
            throw ReservationException.forbidden("담당 선생님만 처리할 수 있습니다.");
        }

        // 같은 시간 예약을 잠근 뒤 상태를 봐야 동시에 들어온 수락이 둘 다 통과하지 않는다.
        List<CourseEntity> slotReservations = courseRepository.findAllForUpdateByDateAndPeriodAndTeacherId(
                slot.date(), slot.period(), teacherId);
        CourseEntity entity = slotReservations.stream()
                .filter(reservation -> reservation.getReservation_id().equals(reservationId))
                .findFirst()
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        if (entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }
        if (entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("이미 처리된 예약입니다.");
        }
        validateSlotNotTaken(entity, slotReservations);

        entity.setState(StateEnum.RESERVED);
        User submitter = findSubmitter(entity);
        notificationService.notifyUser(
                submitter,
                NotificationType.COUNSELING_APPROVED,
                "상담 신청 승인",
                entity.getTeacher().getName() + " 선생님이 상담 신청을 승인했습니다.",
                entity.getReservation_id(),
                "/student/course/" + entity.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(entity.getDate())
        );
        publishReservationChange(entity, submitter.getId(), "APPROVED");
        cancelOtherWaitingReservations(entity, slotReservations);
    }

    /** 한 학생의 신청을 수락하면, 같은 시간에 대기 중이던 다른 학생들의 신청은 자동으로 취소하고 알린다. */
    private void cancelOtherWaitingReservations(CourseEntity accepted, List<CourseEntity> slotReservations) {
        for (CourseEntity other : slotReservations) {
            if (other.getReservation_id().equals(accepted.getReservation_id())
                    || other.getState() != StateEnum.WAITING) {
                continue;
            }

            other.setState(StateEnum.CANCEL);
            User otherSubmitter = findSubmitter(other);
            notificationService.notifyUser(
                    otherSubmitter,
                    NotificationType.COUNSELING_AUTO_CANCELED,
                    "상담 신청 자동 취소",
                    accepted.getTeacher().getName() + " 선생님이 같은 시간에 다른 학생의 상담을 수락하여 신청이 취소되었습니다.",
                    other.getReservation_id(),
                    "/student/course/" + other.getReservation_id(),
                    notificationExpirationService.counselingExpiresAt(other.getDate())
            );
            publishReservationChange(other, otherSubmitter.getId(), "AUTO_CANCELED");
        }
    }

    @Transactional
    public void reject(Long reservationId, Long teacherId) {
        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        if (!entity.getTeacher().getId().equals(teacherId)) {
            throw ReservationException.forbidden("담당 선생님만 처리할 수 있습니다.");
        }
        // 잠금은 신청자가 없어 거절 알림을 보낼 수 없다. 해제는 unlock으로 처리한다.
        if (entity.getState() == StateEnum.CANCEL || entity.getState() == StateEnum.LOCKED) {
            throw ReservationException.conflict("거절할 수 없는 예약입니다.");
        }

        entity.setState(StateEnum.CANCEL);
        User submitter = findSubmitter(entity);
        notificationService.notifyUser(
                submitter,
                NotificationType.COUNSELING_REJECTED,
                "상담 신청 거절",
                entity.getTeacher().getName() + " 선생님이 상담 신청을 거절했습니다.",
                entity.getReservation_id(),
                "/student/course/" + entity.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(entity.getDate())
        );
        publishReservationChange(entity, submitter.getId(), "REJECTED");
    }

    /** 선생님이 수락하지 않은 채 상담 시작 시각이 지난 신청을 취소하고 학생에게 알린다. 취소한 개수를 돌려준다. */
    @Transactional
    public int expireWaitingReservations(LocalDateTime now) {
        List<Long> startedIds = courseRepository.findAllByStateAndDateLessThanEqual(StateEnum.WAITING, now.toLocalDate())
                .stream()
                .filter(entity -> hasCounselingStarted(entity.getDate(), entity.getPeriod(), now))
                .map(CourseEntity::getReservation_id)
                .toList();
        if (startedIds.isEmpty()) {
            return 0;
        }

        List<CourseEntity> expired = courseRepository.findAllForUpdateByIdInAndState(startedIds, StateEnum.WAITING);
        for (CourseEntity entity : expired) {
            entity.setState(StateEnum.CANCEL);
            User submitter = findSubmitter(entity);
            notifyExpired(entity, submitter);
            publishReservationChange(entity, submitter.getId(), "EXPIRED");
        }
        return expired.size();
    }

    /** 표에 없는 교시는 시작 시각을 모르므로 날짜가 지나야 시작된 것으로 본다. */
    private boolean hasCounselingStarted(LocalDate date, String period, LocalDateTime now) {
        return CounselingPeriod.from(period)
                .map(schedule -> !now.isBefore(schedule.startsAt(date)))
                .orElse(date.isBefore(now.toLocalDate()));
    }

    private void notifyExpired(CourseEntity entity, User submitter) {
        LocalDateTime expiresAt = notificationExpirationService.counselingExpiresAt(entity.getDate());
        // 알림 보관 기간까지 지난 오래된 신청은 알려도 곧바로 지워지므로 취소만 한다.
        if (!expiresAt.isAfter(notificationExpirationService.now())) {
            return;
        }

        notificationService.notifyUser(
                submitter,
                NotificationType.COUNSELING_EXPIRED,
                "상담 신청 만료",
                entity.getDate() + " " + entity.getPeriod() + " 상담 신청이 선생님의 수락 없이 상담 시간이 되어 취소되었습니다.",
                entity.getReservation_id(),
                "/student/course/" + entity.getReservation_id(),
                expiresAt
        );
    }

    @Transactional
    public void teacherRock(LockDTO dto, Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        List<CourseEntity> rockList = courseRepository.findAllByDateAndPeriodAndTeacher_Id(
                dto.getDate(), dto.getPeriod(), teacher.getId());

        for (CourseEntity entity : rockList) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
        }

        courseRepository.save(dto.toEntity(teacher));
    }

    /**
     * 휴업일의 모든 교시를 진로 선생님마다 잠근다. 이미 잠긴 시간은 건너뛴다.
     * 잠그는 시간에 걸린 신청은 선생님이 직접 잠글 때처럼 취소한다.
     *
     * @return 새로 잠근 시간 수
     */
    @Transactional
    public int lockHolidays(Collection<LocalDate> holidays) {
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        if (holidays.isEmpty() || teachers.isEmpty()) {
            return 0;
        }

        Map<ReservationSlot, List<CourseEntity>> reservationsBySlot = courseRepository
                .findAllByDateInAndTeacher_IdIn(holidays, teachers.stream().map(User::getId).toList()).stream()
                .collect(Collectors.groupingBy(entity -> new ReservationSlot(
                        entity.getDate(), entity.getPeriod(), entity.getTeacher().getId())));

        List<CourseEntity> locks = new ArrayList<>();
        for (LocalDate date : holidays) {
            for (User teacher : teachers) {
                for (CounselingPeriod period : CounselingPeriod.values()) {
                    List<CourseEntity> reservations = reservationsBySlot.getOrDefault(
                            new ReservationSlot(date, period.getLabel(), teacher.getId()), List.of());
                    if (reservations.stream().anyMatch(entity -> entity.getState() == StateEnum.LOCKED)) {
                        continue;
                    }

                    reservations.forEach(entity -> entity.setState(StateEnum.CANCEL));
                    locks.add(CourseEntity.builder()
                            .teacher(teacher)
                            .date(date)
                            .period(period.getLabel())
                            .state(StateEnum.LOCKED)
                            .build());
                }
            }
        }

        courseRepository.saveAll(locks);
        return locks.size();
    }

    /**
     * 잠가 둔 시간을 다시 예약 가능하게 되돌린다.
     * 잠금과 함께 취소된 예약은 되살리지 않는다. 학생이 다시 신청해야 한다.
     */
    @Transactional
    public void teacherUnlock(LockDTO dto, Long teacherId) {
        validateNotHolidayLock(dto.getDate());

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        List<CourseEntity> locked = courseRepository.findAllByDateAndPeriodAndTeacher_Id(
                        dto.getDate(), dto.getPeriod(), teacher.getId()).stream()
                .filter(entity -> entity.getState() == StateEnum.LOCKED)
                .toList();

        if (locked.isEmpty()) {
            throw ReservationException.notFound("잠긴 시간이 아닙니다.");
        }

        for (CourseEntity entity : locked) {
            entity.setState(StateEnum.CANCEL);
        }
    }

    /**
     * 선생님: 요일·교시를 매주 반복해서 잠근다. 수업처럼 매주 겹치는 일정을 등록할 때 쓴다.
     * 앞으로 이 요일·교시에 잡혀 있는 대기중·수락된 신청은 잠금과 함께 취소한다.
     */
    @Transactional
    public WeeklyLockResponseDTO lockWeekly(WeeklyLockDTO dto, Long teacherId) {
        validateWeeklyLockRequest(dto);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        if (weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(
                teacherId, dto.getDayOfWeek(), dto.getPeriod())) {
            throw ReservationException.conflict("이미 매주 잠가 둔 시간입니다.");
        }

        cancelFutureReservationsOnWeekday(dto.getDayOfWeek(), dto.getPeriod(), teacherId);

        CourseWeeklyLockEntity entity = weeklyLockRepository.save(CourseWeeklyLockEntity.builder()
                .teacher(teacher)
                .dayOfWeek(dto.getDayOfWeek())
                .period(dto.getPeriod())
                .build());
        return WeeklyLockResponseDTO.from(entity);
    }

    /**
     * 선생님: 매주 반복 잠금을 해제한다.
     * 잠금과 함께 취소된 신청은 되살리지 않는다. 학생이 다시 신청해야 한다.
     */
    @Transactional
    public void unlockWeekly(WeeklyLockDTO dto, Long teacherId) {
        CourseWeeklyLockEntity entity = weeklyLockRepository
                .findByTeacher_IdAndDayOfWeekAndPeriod(teacherId, dto.getDayOfWeek(), dto.getPeriod())
                .orElseThrow(() -> ReservationException.notFound("매주 잠근 시간이 아닙니다."));

        weeklyLockRepository.delete(entity);
    }

    /** 선생님: 내가 매주 반복 잠가 둔 요일·교시 목록 */
    @Transactional(readOnly = true)
    public List<WeeklyLockResponseDTO> listWeeklyLocks(Long teacherId) {
        return weeklyLockRepository.findAllByTeacher_Id(teacherId).stream()
                .map(WeeklyLockResponseDTO::from)
                .toList();
    }

    /** 매주 반복 잠금을 새로 걸 때, 오늘 이후 그 요일에 이미 잡혀 있는 신청을 취소한다. */
    private void cancelFutureReservationsOnWeekday(DayOfWeek dayOfWeek, String period, Long teacherId) {
        LocalDate today = LocalDate.now(clock);
        for (CourseEntity entity : courseRepository.findAllByPeriodAndTeacher_IdAndDateGreaterThanEqual(
                period, teacherId, today)) {
            if (entity.getDate().getDayOfWeek() != dayOfWeek) {
                continue;
            }
            if (!isFutureReservationForWeeklyLock(entity.getDate(), period)) {
                continue;
            }
            if (entity.getState() == StateEnum.WAITING || entity.getState() == StateEnum.RESERVED) {
                entity.setState(StateEnum.CANCEL);
            }
        }
    }

    private boolean isFutureReservationForWeeklyLock(LocalDate date, String period) {
        CounselingPeriod schedule = CounselingPeriod.from(period).orElse(null);
        if (schedule == null) {
            return date.isAfter(LocalDate.now(clock));
        }
        return LocalDateTime.now(clock).isBefore(schedule.startsAt(date));
    }

    private void validateWeeklyLockRequest(WeeklyLockDTO dto) {
        if (dto.getDayOfWeek() == null) {
            throw ReservationException.badRequest("요일을 선택해주세요.");
        }
        if (dto.getPeriod() == null || dto.getPeriod().isBlank()) {
            throw ReservationException.badRequest("교시를 선택해주세요.");
        }
    }

    /** 선생님이 매주 반복으로 잠가 둔 요일·교시에는 신청할 수 없다. */
    private void validateNotWeeklyLocked(LocalDate date, String period, Long teacherId) {
        if (weeklyLockRepository.existsByTeacher_IdAndDayOfWeekAndPeriod(teacherId, date.getDayOfWeek(), period)) {
            throw ReservationException.locked("선생님이 매주 잠가 둔 시간입니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherReadDTO> T_Read(Long id) {
        return courseRepository.findByTeacher_IdAndStateOrderByDateAscPeriodAsc(id, StateEnum.RESERVED).stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherReadDTO> P_Read(Long id) {
        return courseRepository.findByTeacher_IdAndStateOrderByDateAscPeriodAsc(id, StateEnum.WAITING).stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentReadDTO> S_Read(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        return courseRepository.findBySubmitterHash(cryptoService.submitterHash(id)).stream()
                .map(e -> new StudentReadDTO(
                        e.getReservation_id(),
                        user.getName(),
                        e.getTeacher().getId(),
                        e.getTeacher().getName(),
                        e.getDate(),
                        e.getPeriod(),
                        e.getCategory(),
                        ReservationStatus.from(e.getState())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotStatusDTO> readSlotStatus(Long teacherId, LocalDate date, String period, Long viewerId) {
        if (date == null) {
            throw ReservationException.badRequest("날짜를 선택해주세요.");
        }
        // 휴업일은 스케줄러가 LOCKED 행으로 잠가 두므로 조회할 때는 나이스를 부르지 않는다.

        User teacher = findTeacher(teacherId);

        List<CourseEntity> reservations = (period == null || period.isBlank())
                ? courseRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, teacher.getId())
                : courseRepository.findAllByDateAndPeriodAndTeacher_Id(date, period, teacher.getId());

        Map<String, StateEnum> stateByPeriod = new LinkedHashMap<>();
        for (CourseWeeklyLockEntity rule : weeklyLockRepository.findAllByTeacher_Id(teacher.getId())) {
            if (rule.getDayOfWeek() != date.getDayOfWeek()) {
                continue;
            }
            if (period == null || period.isBlank() || rule.getPeriod().equals(period.trim())) {
                stateByPeriod.put(rule.getPeriod(), StateEnum.LOCKED);
            }
        }

        for (CourseEntity entity : reservations) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            StateEnum previous = stateByPeriod.get(entity.getPeriod());
            if (previous == null || priority(entity.getState()) > priority(previous)) {
                stateByPeriod.put(entity.getPeriod(), entity.getState());
            }
        }

        Set<String> minePeriods = findMinePeriods(date, period, viewerId);

        return Stream.concat(stateByPeriod.keySet().stream(), minePeriods.stream())
                .distinct()
                .sorted()
                .map(slot -> new SlotStatusDTO(
                        teacher.getId(), date, slot, stateByPeriod.get(slot), minePeriods.contains(slot)))
                .toList();
    }

    /**
     * 보는 학생이 그날 이미 신청해 둔 교시.
     *
     * <p>같은 시간에는 선생님을 바꿔도, 일반 상담이어도 다시 신청할 수 없다.
     * 신청 화면에서 미리 막으려면 이 교시도 함께 내려줘야 한다.
     * 선생님이 조회하면 신청 기록이 없어 비어 있다.</p>
     */
    private Set<String> findMinePeriods(LocalDate date, String period, Long viewerId) {
        if (viewerId == null) {
            return Set.of();
        }

        String submitterHash = cryptoService.submitterHash(viewerId);
        Set<String> periods = new LinkedHashSet<>(courseRepository.findActivePeriods(submitterHash, date));
        periods.addAll(commonRepository.findActivePeriods(submitterHash, date));

        if (period != null && !period.isBlank()) {
            periods.retainAll(Set.of(period.trim()));
        }
        return periods;
    }

    /**
     * 학사일정상 휴업일(공휴일 등)에는 상담을 잡을 수 없다.
     * 학사일정을 확인하지 못하면 휴업일일 수 있으므로 예약을 막는다.
     */
    private void validateNotHoliday(LocalDate date) {
        boolean holiday;
        try {
            holiday = scheduleService.isHoliday(date);
        } catch (BusinessException e) {
            log.warn("학사일정을 확인하지 못해 예약을 막습니다. date={}", date, e);
            throw ReservationException.badGateway(
                    "학사일정을 확인할 수 없어 예약을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }

        if (holiday) {
            throw ReservationException.locked("휴업일에는 상담을 예약할 수 없습니다.");
        }
    }

    private int priority(StateEnum state) {
        return switch (state) {
            case LOCKED -> 4;
            case RESERVED -> 3;
            case AUTO -> 2;
            default -> 1;
        };
    }

    private TeacherReadDTO toTeacherDTO(CourseEntity e) {
        return new TeacherReadDTO(
                e.getReservation_id(),
                cryptoService.decrypt(e.getEncryptedUserName()),
                cryptoService.decrypt(e.getEncryptedStudentNumber()),
                e.getDate(),
                e.getPeriod(),
                e.getCategory(),
                cryptoService.decrypt(e.getEncryptedTitle()),
                cryptoService.decrypt(e.getEncryptedContent())
        );
    }

    private void validateCategory(CounselingCategoryEnum category) {
        if (category == null) {
            throw ReservationException.badRequest("상담 분야를 선택해주세요.");
        }
    }

    /**
     * 휴업일 잠금은 매일 다시 만들어지고 예약 신청도 계속 막히므로, 풀렸다고 오해하지 않도록 해제를 막는다.
     * 학사일정을 확인하지 못하면 해제는 허용한다. 예약 신청이 휴업일을 따로 다시 확인한다.
     */
    private void validateNotHolidayLock(LocalDate date) {
        boolean holiday;
        try {
            holiday = scheduleService.isHoliday(date);
        } catch (BusinessException e) {
            log.warn("학사일정을 확인하지 못해 휴업일 여부를 보지 않고 잠금을 해제합니다. date={}", date, e);
            return;
        }

        if (holiday) {
            throw ReservationException.locked("휴업일은 잠금을 해제할 수 없습니다.");
        }
    }

    /** 한 선생님이 같은 날 같은 교시에 두 건을 수락하지 못하게 막는다. */
    private void validateSlotNotTaken(CourseEntity target, List<CourseEntity> slotReservations) {
        boolean taken = slotReservations.stream()
                .filter(other -> !other.getReservation_id().equals(target.getReservation_id()))
                .anyMatch(other -> other.getState() == StateEnum.RESERVED);

        if (taken) {
            throw ReservationException.conflict("같은 시간에 이미 수락한 상담이 있습니다.");
        }
    }

    /** 상담 시작이 임박하면 학생이 취소할 수 없다. */
    private void validateCancelDeadline(LocalDate date, String period) {
        CounselingPeriod schedule = CounselingPeriod.from(period).orElse(null);
        if (schedule == null) {
            // 시간표에 없는 교시는 시작 시각을 알 수 없다. 취소를 막지 않는다.
            log.warn("시간표에 없는 교시라 취소 마감을 확인하지 못했습니다. period={}", period);
            return;
        }

        LocalDateTime deadline = schedule.startsAt(date).minus(CANCEL_DEADLINE);
        if (!LocalDateTime.now(clock).isBefore(deadline)) {
            throw ReservationException.conflict("상담 시작 1시간 전부터는 취소할 수 없습니다.");
        }
    }

    private User findSubmitter(CourseEntity entity) {
        Long submitterId = Long.valueOf(cryptoService.decrypt(entity.getEncryptedUserId()));
        return userRepository.findById(submitterId)
                .orElseThrow(() -> ReservationException.notFound("상담 신청 학생을 찾을 수 없습니다."));
    }

    private User findTeacher(Long teacherId, Long studentId) {
        if (teacherId != null && teacherId.equals(studentId)) {
            throw ReservationException.badRequest("자기 자신을 선생님으로 지정할 수 없습니다.");
        }
        return findTeacher(teacherId);
    }

    private User findTeacher(Long teacherId) {
        if (teacherId == null) {
            throw ReservationException.badRequest("선생님을 선택해주세요.");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("선생님을 찾을 수 없습니다."));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw ReservationException.badRequest("선생님 계정만 선택할 수 있습니다.");
        }
        return teacher;
    }

    private User findStudent(Long studentId) {
        if (studentId == null) {
            throw ReservationException.badRequest("학생을 선택해주세요.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ReservationException.notFound("학생을 찾을 수 없습니다."));
        if (student.getRole() != UserRole.STUDENT) {
            throw ReservationException.badRequest("학생 계정만 선택할 수 있습니다.");
        }
        return student;
    }

    private User findStudentForUpdate(Long studentId) {
        if (studentId == null) {
            throw ReservationException.badRequest("학생을 선택해주세요.");
        }

        User student = userRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> ReservationException.notFound("학생을 찾을 수 없습니다."));
        if (student.getRole() != UserRole.STUDENT) {
            throw ReservationException.badRequest("학생 계정만 선택할 수 있습니다.");
        }
        return student;
    }

    private void publishReservationChange(CourseEntity entity, Long studentId, String action) {
        Long teacherId = entity.getTeacher() == null ? null : entity.getTeacher().getId();
        reservationRealtimeService.sendAfterCommit(
                Arrays.asList(studentId, teacherId),
                new ReservationRealtimeEvent(
                        "COURSE",
                        action,
                        entity.getReservation_id(),
                        entity.getDate(),
                        entity.getPeriod(),
                        ReservationStatus.from(entity.getState()).name(),
                        teacherId,
                        studentId
                )
        );
    }

    private String validateReservationSlot(LocalDate date, String period) {
        if (date == null) {
            throw ReservationException.badRequest("날짜를 선택해주세요.");
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw ReservationException.badRequest("지난 날짜에는 상담을 신청할 수 없습니다.");
        }

        CounselingPeriod counselingPeriod = CounselingPeriod.from(period)
                .orElseThrow(() -> ReservationException.badRequest("존재하지 않는 교시입니다."));
        if (counselingPeriod.hasStarted(date, clock)) {
            throw ReservationException.badRequest("이미 시작된 교시는 신청할 수 없습니다.");
        }
        return counselingPeriod.getLabel();
    }

    private void validateBeforeApplyDeadline(LocalDate date, String period) {
        LocalDateTime startsAt = CounselingPeriod.from(period).orElseThrow().startsAt(date);
        if (!LocalDateTime.now(clock).isBefore(startsAt.minus(APPLY_DEADLINE))) {
            throw ReservationException.badRequest("상담 시작 30분 전부터는 신청할 수 없습니다.");
        }
    }
}
