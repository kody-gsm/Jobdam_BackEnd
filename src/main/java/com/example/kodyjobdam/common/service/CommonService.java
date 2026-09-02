package com.example.kodyjobdam.common.service;


import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.response.StudentReadDTO;
import com.example.kodyjobdam.common.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonService {

    private final CommonRepository commonRepository;

    private final UserRepository userRepository;

    public void commonSave(CommonEntity entity) {
        commonRepository.save(entity);
    }

    public void createReservation(CreateDTO dto, Long id) {

        User userId = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        if (dto.getTeacherId() == null) {
            throw ReservationException.badRequest("선생님을 선택해야 합니다.");
        }

        User teacher = userRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> ReservationException.notFound("선생님을 찾을 수 없습니다."));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw ReservationException.badRequest("선생님이 아닙니다.");
        }

        // 학생은 같은 교시에 한 번만 신청할 수 있다 (선생님과 무관하게 전체를 확인)
        for (CommonEntity entity : commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod())) {
            if (entity.getUser() != null &&
                    entity.getUser().getId().equals(id) &&
                    entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
        }

        // 슬롯은 선생님별로 나뉘므로 해당 선생님의 그 시간대만 확인한다
        for (CommonEntity entity : commonRepository.findAllByDateAndPeriodAndTeacherId(
                dto.getDate(), dto.getPeriod(), teacher.getId())) {
            if (entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {
                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        commonSave(dto.toEntity(userId));
    }

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

    public void allow(Long reservationId, Long teacherId) {

        log.info("start");

        CommonEntity entity = commonRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        log.info("first");

        // 나에게 신청된 예약만 수락할 수 있다
        if (!teacherId.equals(entity.getTeacherId())) {
            throw ReservationException.forbidden("본인에게 신청된 예약이 아닙니다.");
        }

        if(entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }

        // 잠긴 시간(LOCKED)이나 이미 수락한 예약(RESERVED)이 수락되지 않도록 막는다
        if(entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("수락할 수 있는 예약이 아닙니다.");
        }

        log.info("second");

        entity.setState(StateEnum.RESERVED);

        commonSave(entity);
    }

    public void teacherRock(LockDTO dto, Long teacherId) {
        // 잠금은 본인 시간대에만 적용된다
        List<CommonEntity> rockList = commonRepository.findAllByDateAndPeriodAndTeacherId(
                dto.getDate(), dto.getPeriod(), teacherId);

        for (CommonEntity entity : rockList) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
            commonSave(entity);
        }

        commonSave(dto.toEntity(teacherId));
    }

    public List<TeacherReadDTO> T_Read(Long id) { //수락한 예약
        List<CommonEntity> entity = commonRepository.findByTeacherIdAndStateOrderByDateAscPeriodAsc(id, StateEnum.RESERVED);

        return entity.stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    public List<TeacherReadDTO> P_Read(Long id) { //나에게 신청된 수락 대기중인 예약
        List<CommonEntity> entity = commonRepository.findByTeacherIdAndStateOrderByDateAscPeriodAsc(id, StateEnum.WAITING);

        return entity.stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    private TeacherReadDTO toTeacherDTO(CommonEntity e) {
        return new TeacherReadDTO(
                e.getReservation_id(),
                e.getUser().getName(),
                e.getDate(),
                e.getPeriod()
        );
    }

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
}
