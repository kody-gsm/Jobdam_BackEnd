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

        //여기에 나중에 토큰에서 빼온 id를 매개변수로
        List<CommonEntity> CreateList = commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod()); //여기 state확인 해야함. RESERVED인가

        User userId = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        if (CreateList.isEmpty()) {
            commonSave(dto.toEntity(userId));
        }
        else {
            for (CommonEntity entity : CreateList) {
                if(entity.getState() == StateEnum.LOCKED) {
                    throw ReservationException.locked("잠긴 날짜 입니다.");
                }
                if (
                        entity.getUser().getId().equals(id) &&
                        entity.getState() != StateEnum.CANCEL) {
                    throw ReservationException.conflict("이미 예약한 시간입니다.");
                }
                if (entity.getState() == StateEnum.RESERVED) {

                    throw ReservationException.conflict("누군가 예약한 시간입니다.");
                }
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

        if(entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }

        log.info("second");

        entity.setState(StateEnum.RESERVED);
        entity.setTeacher_id(teacherId);

        commonSave(entity);
    }

    public void teacherRock(LockDTO dto) {
        List<CommonEntity> rockList = commonRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());

        if(rockList.isEmpty()) {
           commonSave(dto.toEntity(dto));
        }
        for(CommonEntity entity : rockList) {
            if(entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
        }

        commonSave(dto.toEntity(dto));
    }

    public List<TeacherReadDTO> T_Read(Long id) { //수락한 예약
        List<CommonEntity> entity = commonRepository.findByUser_id(id);

        return entity.stream()
                .map(e -> new TeacherReadDTO(
                        e.getReservation_id(),
                        e.getUser().getName(),
                        e.getDate(),
                        e.getPeriod()
                ))
                .toList();
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
