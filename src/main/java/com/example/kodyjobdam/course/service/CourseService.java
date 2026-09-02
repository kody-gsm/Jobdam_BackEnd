package com.example.kodyjobdam.course.service;

import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.dto.request.LockDTO;
import com.example.kodyjobdam.course.dto.response.StudentReadDTO;
import com.example.kodyjobdam.course.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    private final UserRepository userRepository;

    public void courseSave(CourseEntity entity) {
        courseRepository.save(entity);
    }

    public void createReservation(CreateDTO dto, Long id) {
        List<CourseEntity> createList = courseRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());

        User userId = userRepository.findById(id)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));

        for (CourseEntity entity : createList) {
            if (entity.getState() == StateEnum.LOCKED) {
                throw ReservationException.locked("잠긴 날짜 입니다.");
            }
            if (entity.getUser().getId().equals(id) && entity.getState() != StateEnum.CANCEL) {
                throw ReservationException.conflict("이미 예약한 시간입니다.");
            }
            if (entity.getState() == StateEnum.RESERVED) {
                throw ReservationException.conflict("누군가 예약한 시간입니다.");
            }
        }

        courseSave(dto.toEntity(userId));
    }

    public void cancelReservation(Long reservationId, Long userId) {
        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() ->
                        ReservationException.notFound("취소 할 수 없습니다."));

        if (!entity.getUser().getId().equals(userId)) {
            throw ReservationException.forbidden("권한이 없습니다.");
        }

        entity.setState(StateEnum.CANCEL);

        courseSave(entity);
    }

    public void allow(Long reservationId, Long teacherId) {
        log.info("start");

        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() -> ReservationException.notFound("값을 찾을 수 없습니다."));

        log.info("first");

        if (entity.getState() == StateEnum.CANCEL) {
            throw ReservationException.notFound("이미 취소된 에약입니다.");
        }

        // 잠긴 시간(LOCKED)이나 이미 수락한 예약(RESERVED)이 수락되지 않도록 막는다
        if (entity.getState() != StateEnum.WAITING) {
            throw ReservationException.conflict("수락할 수 있는 예약이 아닙니다.");
        }

        log.info("second");

        entity.setState(StateEnum.RESERVED);
        entity.setTeacher_id(teacherId);

        courseSave(entity);
    }

    public void teacherRock(LockDTO dto) {
        List<CourseEntity> rockList = courseRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());

        for (CourseEntity entity : rockList) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
            courseSave(entity);
        }

        courseSave(dto.toEntity(dto));
    }

    public List<TeacherReadDTO> T_Read(Long id) {
        List<CourseEntity> entity = courseRepository.findByUser_id(id);

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
        List<CourseEntity> entity = courseRepository.findByUser_id(id);

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
