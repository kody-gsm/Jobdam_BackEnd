package com.example.kodyjobdam.course.controller;

import com.example.kodyjobdam.course.dto.request.CreateDTO;
import com.example.kodyjobdam.course.dto.request.LockDTO;
import com.example.kodyjobdam.course.dto.response.StudentReadDTO;
import com.example.kodyjobdam.course.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.course.service.CourseService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    private final SecurityUtil securityUtil;

    private final UserRepository userRepository;

    @PostMapping("/student/course")
    public ResponseEntity<?> createReservation(@RequestBody CreateDTO dto) {
        courseService.createReservation(dto, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("선생님께서 요청 검토중 입니다.");
    }

    @PatchMapping("/student/course/cancel/{reservation_id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservation_id) {
        courseService.cancelReservation(reservation_id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("취소되었습니다.");
    }

    @PatchMapping("/teacher/course/allow/{id}")
    public ResponseEntity<?> reservationAllow(@PathVariable Long id) {
        courseService.allow(id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("요청을 수락했습니다.");
    }

    @PostMapping("/teacher/course/lock")
    public ResponseEntity<?> teacherRock(@RequestBody LockDTO dto) {
        courseService.teacherRock(dto);
        return ResponseEntity.ok().body("해당 시간을 잠궜습니다.");
    }

    @GetMapping("/student/course")
    public List<StudentReadDTO> S_read() {
        return courseService.S_Read(securityUtil.getCurrentUserId());
    }

    @GetMapping("/teacher/course")
    public List<TeacherReadDTO> T_read() {
        return courseService.T_Read(securityUtil.getCurrentUserId());
    }

    @GetMapping("/teacher") //여기에 선생님 id 3개를 받아오는
    public List<Integer> Id_read() {
        return userRepository.findByRole(UserRole.TEACHER);
    }
}
