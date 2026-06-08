package com.example.kodyjobdam.common.controller;

import com.example.kodyjobdam.common.dto.request.CreateDTO;
import com.example.kodyjobdam.common.dto.request.LockDTO;
import com.example.kodyjobdam.common.dto.response.StudentReadDTO;
import com.example.kodyjobdam.common.dto.response.TeacherReadDTO;
import com.example.kodyjobdam.common.service.CommonService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommonController {

    private final CommonService commonService;

    private final SecurityUtil securityUtil;

    @PostMapping("/student/common")
    public ResponseEntity<?> createReservation(@RequestBody CreateDTO dto) {
        commonService.createReservation(dto, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("선생님께서 요청 검토중 입니다.");
    }

    @PatchMapping("/student/common/cancel/{reservation_id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long reservation_id) {
        commonService.cancelReservation(reservation_id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("취소되었습니다.");
    }

    @PatchMapping("/teacher/common/allow/{reservation_id}")
    public ResponseEntity<?> reservationAllow(@PathVariable Long reservation_id) {
        commonService.allow(reservation_id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("요청을 수락했습니다.");
    }

    @PostMapping("/teacher/common/lock")
    public ResponseEntity<?> teacherRock(@RequestBody LockDTO dto) {
        commonService.teacherRock(dto);
        return ResponseEntity.ok().body("해당 시간을 잠궜습니다.");
    }

    @GetMapping("/student/common/read")
    public List<StudentReadDTO> studentRecord() {
        return commonService.studentRecord(securityUtil.getCurrentUserId());
    }

    @GetMapping("/teacher/common/read")
    public List<TeacherReadDTO> teacherRecord() {
        return commonService.teacherRecord(securityUtil.getCurrentUserId());
    }

    @GetMapping("/student/common/record")
    public List<StudentReadDTO> selectStudentRecord() {
        return commonService.selectRecordStatus(securityUtil.getCurrentUserId());
    }
}