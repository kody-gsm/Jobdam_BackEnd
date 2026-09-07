package com.example.kodyjobdam.common.service;

import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.response.StudentReadDTO;
import com.example.kodyjobdam.common.dto.response.SlotStatusDTO;
import com.example.kodyjobdam.common.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {

    private final CommonRepository commonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationExpirationService notificationExpirationService;
    private final CounselingReservationCryptoService cryptoService;
    private final ScheduleService scheduleService;

    /** 상시 잠금 교시(예: 점심시간). 쉼표로 여러 개를 지정할 수 있다. */
    @Value("${reservation.locked-periods:}")
    private Set<String> lockedPeriods;

    public void commonSave(CommonEntity entity) {
        commonRepository.save(entity);
    }

    @Transactional
    public void createReservation(CreateDTO dto, Long id) {
        validateNotHoliday(dto.getDate());
        validateNotLockedPeriod(dto.getPeriod());

        User user = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        User teacher = findTeacher(dto.getTeacherId(), id);
        validateCategory(dto.getCategory());
        String submitterHash = cryptoService.submitterHash(id);

        for (CommonEntity entity : commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())) {
            if (submitterHash.equals(entity.getSubmitterHash())
                    && entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
        }

        for (CommonEntity entity : commonRepository.findAllByDateAndPeriodAndTeacher_Id(
                dto.getDate(), dto.getPeriod(), teacher.getId())) {
            if (entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {
                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        CommonEntity reservation = commonRepository.save(dto.toEntity(
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
                NotificationType.COMMON_COUNSELING_REQUESTED,
                "새로운 상담 신청",
                "학생이 상담을 신청했습니다.",
                reservation.getReservation_id(),
                "/teacher/common/" + reservation.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(reservation.getDate())
        );
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("취소 할 수 없습니다."));

        if (!cryptoService.submitterHash(userId).equals(entity.getSubmitterHash())) {
            throw ReservationException.forbidden("권한이 없습니다.");
        }

        entity.setState(StateEnum.CANCEL);
    }

    @Transactional
    public void allow(Long reservationId, Long teacherId) {
        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        if (entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }
        if (!entity.getTeacher().getId().equals(teacherId)) {
            throw ReservationException.forbidden("담당 선생님만 처리할 수 있습니다.");
        }
        if (entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("이미 처리된 예약입니다.");
        }

        entity.setState(StateEnum.RESERVED);
        User submitter = findSubmitter(entity);
        notificationService.notifyUser(
                submitter,
                NotificationType.COUNSELING_APPROVED,
                "상담 신청 승인",
                entity.getTeacher().getName() + " 선생님이 상담 신청을 승인했습니다.",
                entity.getReservation_id(),
                "/student/common/" + entity.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(entity.getDate())
        );
    }

    @Transactional
    public void reject(Long reservationId, Long teacherId) {
        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        if (!entity.getTeacher().getId().equals(teacherId)) {
            throw ReservationException.forbidden("담당 선생님만 처리할 수 있습니다.");
        }
        if (entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("이미 처리된 예약입니다.");
        }

        entity.setState(StateEnum.CANCEL);
        User submitter = findSubmitter(entity);
        notificationService.notifyUser(
                submitter,
                NotificationType.COUNSELING_REJECTED,
                "상담 신청 거절",
                entity.getTeacher().getName() + " 선생님이 상담 신청을 거절했습니다.",
                entity.getReservation_id(),
                "/student/common/" + entity.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(entity.getDate())
        );
    }

    @Transactional
    public void teacherRock(LockDTO dto, Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        List<CommonEntity> rockList = commonRepository.findAllByDateAndPeriodAndTeacher_Id(
                dto.getDate(), dto.getPeriod(), teacher.getId());

        for (CommonEntity entity : rockList) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
        }

        commonRepository.save(dto.toEntity(teacher));
    }

    @Transactional(readOnly = true)
    public List<TeacherReadDTO> T_Read(Long id) {
        return commonRepository.findByTeacher_IdAndStateOrderByDateAscPeriodAsc(id, StateEnum.RESERVED).stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherReadDTO> P_Read(Long id) {
        return commonRepository.findByTeacher_IdAndStateOrderByDateAscPeriodAsc(id, StateEnum.WAITING).stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentReadDTO> S_Read(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        return commonRepository.findBySubmitterHash(cryptoService.submitterHash(id)).stream()
                .map(e -> new StudentReadDTO(
                        e.getReservation_id(),
                        user.getName(),
                        e.getDate(),
                        e.getPeriod(),
                        e.getCategory()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotStatusDTO> readSlotStatus(Long teacherId, LocalDate date, String period) {
        if (date == null) {
            throw ReservationException.badRequest("날짜를 선택해주세요.");
        }
        validateNotHoliday(date);

        User teacher = findTeacher(teacherId);

        List<CommonEntity> reservations = (period == null || period.isBlank())
                ? commonRepository.findAllByDateAndTeacher_IdOrderByPeriodAsc(date, teacher.getId())
                : commonRepository.findAllByDateAndPeriodAndTeacher_Id(date, period, teacher.getId());

        Map<String, StateEnum> stateByPeriod = new LinkedHashMap<>();
        for (String lockedPeriod : lockedPeriods) {
            if (lockedPeriod.isBlank()) {
                continue;
            }
            if (period == null || period.isBlank() || lockedPeriod.equals(period.trim())) {
                stateByPeriod.put(lockedPeriod, StateEnum.LOCKED);
            }
        }

        for (CommonEntity entity : reservations) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            StateEnum previous = stateByPeriod.get(entity.getPeriod());
            if (previous == null || priority(entity.getState()) > priority(previous)) {
                stateByPeriod.put(entity.getPeriod(), entity.getState());
            }
        }

        return stateByPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SlotStatusDTO(teacher.getId(), date, e.getKey(), e.getValue()))
                .toList();
    }

    /** 학사일정상 휴업일(공휴일 등)에는 상담을 잡을 수 없다. */
    private void validateNotHoliday(LocalDate date) {
        if (scheduleService.isHoliday(date)) {
            throw ReservationException.locked("휴업일에는 상담을 예약할 수 없습니다.");
        }
    }

    /** 상시 잠금으로 지정된 교시에는 상담을 잡을 수 없다. */
    private void validateNotLockedPeriod(String period) {
        if (period != null && !period.isBlank() && lockedPeriods.contains(period.trim())) {
            throw ReservationException.locked(period.trim() + "교시에는 상담을 예약할 수 없습니다.");
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

    private TeacherReadDTO toTeacherDTO(CommonEntity e) {
        return new TeacherReadDTO(
                e.getReservation_id(),
                cryptoService.decrypt(e.getEncryptedUserName()),
                e.getDate(),
                e.getPeriod(),
                e.getCategory()
        );
    }

    private void validateCategory(CounselingCategoryEnum category) {
        if (category == null) {
            throw ReservationException.badRequest("상담 분야를 선택해주세요.");
        }
    }

    private User findSubmitter(CommonEntity entity) {
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
}
