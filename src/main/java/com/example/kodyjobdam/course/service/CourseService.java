package com.example.kodyjobdam.course.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseService {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*시?");

    private final CourseRepository courseRepository;

    private final UserRepository userRepository;

    public void courseSave(CourseEntity entity) {
        courseRepository.save(entity);
    }

    public void createReservation(CreateDTO dto, Long id) {
        List<CourseEntity> createList = courseRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());

        User userId = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 없습니다."));

        if (createList.isEmpty()) {
            courseSave(dto.toEntity(userId));
        } else {
            for (CourseEntity entity : createList) {
                if (entity.getState() == StateEnum.LOCKED) {
                    throw new ResponseStatusException(HttpStatus.LOCKED, "잠긴 날짜 입니다.");
                }
                if (entity.getUser().getId().equals(id) && entity.getState() != StateEnum.CANCEL) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약한 시간입니다.");
                }
                if (entity.getState() == StateEnum.RESERVED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "누군가 예약한 시간입니다.");
                }
            }
        }
        courseSave(dto.toEntity(userId));
    }

    public void cancelReservation(Long reservationId, Long userId) {
        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "취소 할 수 없습니다."));

        if (!entity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        entity.setState(StateEnum.CANCEL);

        courseSave(entity);
    }

    public void allow(Long reservationId, Long teacherId) {
        log.info("start");

        CourseEntity entity = courseRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "값을 찾을 수 없습니다."));

        log.info("first");

        if (entity.getState() == StateEnum.CANCEL) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미 취소된 에약입니다.");
        }

        // 잠긴 시간(LOCKED)이나 이미 수락한 예약(RESERVED)이 수락되지 않도록 막는다
        if (entity.getState() != StateEnum.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "수락할 수 있는 예약이 아닙니다.");
        }

        log.info("second");

        entity.setState(StateEnum.RESERVED);
        entity.assignTeacher(teacherId);

        courseSave(entity);
    }

    public void teacherRock(LockDTO dto) {
        List<CourseEntity> rockList = courseRepository.findAllByDateAndPeriod(dto.getDate(), dto.getPeriod());

        if (rockList.isEmpty()) {
            courseSave(dto.toEntity(dto));
        }
        for (CourseEntity entity : rockList) {
            if (entity.getState() == StateEnum.CANCEL) {
                continue;
            }
            entity.setState(StateEnum.CANCEL);
        }

        courseSave(dto.toEntity(dto));
    }

    public List<TeacherReadDTO> teacherRecord(Long id) { //수락한 예약
        List<CourseEntity> entity = courseRepository.findByTeacherId(id);

        return entity.stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    public List<TeacherReadDTO> pendingRecord() { //수락 대기중인 예약
        List<CourseEntity> entity = courseRepository.findByStateOrderByDateAscPeriodAsc(StateEnum.WAITING);

        return entity.stream()
                .filter(e -> e.getUser() != null) // 잠긴 시간대는 신청자가 없다
                .map(this::toTeacherDTO)
                .toList();
    }

    private TeacherReadDTO toTeacherDTO(CourseEntity e) {
        return new TeacherReadDTO(
                        e.getReservation_id(),
                        e.getUser().getName(),
                        e.getDate(),
                        e.getPeriod()
        );
    }

    public List<StudentReadDTO> studentRecord(Long id) {
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

    public List<StudentReadDTO> selectRecordStatus(Long id) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<CourseEntity> entity = courseRepository.findByUser_idAndDateGreaterThanEqual(id, today);

        return entity.stream()
                .filter(e -> e.getState() != StateEnum.CANCEL && e.getState() != StateEnum.LOCKED)
                .filter(e -> isFutureReservation(e.getDate(), e.getPeriod(), today, now))
                .map(e -> new StudentReadDTO(
                        e.getReservation_id(),
                        e.getUser().getName(),
                        e.getDate(),
                        e.getPeriod()
                ))
                .toList();
    }

    private boolean isFutureReservation(LocalDate reservationDate, String period, LocalDate today, LocalTime now) {
        if (reservationDate.isAfter(today)) {
            return true;
        }

        return reservationDate.isEqual(today)
                && parseStartTime(period)
                .map(startTime -> startTime.isAfter(now))
                .orElse(false);
    }

    private Optional<LocalTime> parseStartTime(String period) {
        if (period == null) {
            return Optional.empty();
        }

        Matcher matcher = TIME_PATTERN.matcher(period);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return Optional.empty();
        }

        return Optional.of(LocalTime.of(hour, minute));
    }
}
