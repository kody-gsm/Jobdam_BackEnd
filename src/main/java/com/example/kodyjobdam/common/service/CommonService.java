package com.example.kodyjobdam.common.service;


import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.response.StudentReadDTO;
import com.example.kodyjobdam.common.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {

    private final CommonRepository commonRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final NotificationExpirationService notificationExpirationService;

    public void commonSave(CommonEntity entity) {
        commonRepository.save(entity);
    }

    @Transactional
    public void createReservation(CreateDTO dto, Long id) {

        //여기에 나중에 토큰에서 빼온 id를 매개변수로
        List<CommonEntity> CreateList = commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod()); //여기 state확인 해야함. RESERVED인가

        User user = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        User teacher = findTeacher(dto.getTeacherId(), id);

        for (CommonEntity entity : CreateList) {
            if(entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (
                    entity.getUser() != null &&
                    entity.getUser().getId().equals(id) &&
                    entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {

                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        CommonEntity reservation = commonRepository.save(dto.toEntity(user, teacher));
        notificationService.notifyUser(
                teacher,
                NotificationType.COMMON_COUNSELING_REQUESTED,
                "새로운 상담 신청",
                user.getStudent_number() + " " + user.getName() + " 학생이 상담을 신청했습니다.",
                reservation.getReservation_id(),
                "/teacher/common/" + reservation.getReservation_id(),
                notificationExpirationService.counselingExpiresAt(reservation.getDate())
        );
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {

        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() ->
                ReservationException.notFound("취소 할 수 없습니다."));

        if (!entity.getUser().getId().equals(userId)) { // 본인 예약을 본인이 취소하였는지
            throw ReservationException.forbidden("권한이 없습니다.");
        }

        entity.setState(StateEnum.CANCEL);

        commonSave(entity);
    }

    @Transactional
    public void allow(Long reservationId, Long teacherId) {
        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        if(entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }
        if (!entity.getTeacher().getId().equals(teacherId)) {
            throw ReservationException.forbidden("담당 선생님만 처리할 수 있습니다.");
        }
        if (entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("이미 처리된 예약입니다.");
        }

        entity.setState(StateEnum.RESERVED);

        notificationService.notifyUser(
                entity.getUser(),
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
        notificationService.notifyUser(
                entity.getUser(),
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
        List<CommonEntity> rockList = commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        for(CommonEntity entity : rockList) {
            if(entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
            commonSave(entity);
        }

        commonSave(dto.toEntity(dto, teacher));
    }

    @Transactional(readOnly = true)
    public List<TeacherReadDTO> T_Read(Long id) { //수락한 예약
        List<CommonEntity> entity = commonRepository.findByTeacher_Id(id);

        return entity.stream()
                .map(e -> new TeacherReadDTO(
                        e.getReservation_id(),
                        e.getUser().getName(),
                        e.getDate(),
                        e.getPeriod()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentReadDTO> S_Read(Long id) {
        List<CommonEntity> entity = commonRepository.findByUser_id(id);

        return entity.stream()
                .map(e -> new StudentReadDTO(
                        e.getReservation_id(),
                        e.getUser().getName(),
                        e.getDate(),
                        e.getPeriod()
                ))
                .toList();
    }

    private User findTeacher(Long teacherId, Long studentId) {
        if (teacherId == null) {
            throw ReservationException.badRequest("선생님을 선택해주세요.");
        }
        if (teacherId.equals(studentId)) {
            throw ReservationException.badRequest("자기 자신을 선생님으로 지정할 수 없습니다.");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ReservationException.notFound("선생님을 찾을 수 없습니다."));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw ReservationException.badRequest("선생님 계정만 선택할 수 있습니다.");
        }
        return teacher;
    }
}
