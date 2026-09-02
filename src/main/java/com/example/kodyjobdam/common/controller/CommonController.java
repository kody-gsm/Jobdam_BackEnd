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

    @PatchMapping("/teacher/common/allow/{id}")
    public ResponseEntity<?> reservationAllow(@PathVariable Long id) {
        commonService.allow(id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("요청을 수락했습니다.");
    }

    @PatchMapping("/teacher/common/reject/{id}")
    public ResponseEntity<?> reservationReject(@PathVariable Long id) {
        commonService.reject(id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("요청을 거절했습니다.");
    }

    @PostMapping("/teacher/common/lock")
    public ResponseEntity<?> teacherRock(@RequestBody LockDTO dto) {
        commonService.teacherRock(dto, securityUtil.getCurrentUserId());
        return ResponseEntity.ok().body("해당 시간을 잠궜습니다.");
    }

    @GetMapping("/student/common")
    public List<StudentReadDTO> S_read() {
        return commonService.S_Read(securityUtil.getCurrentUserId());
    }

    @GetMapping("/teacher/common")
    public List<TeacherReadDTO> T_read() {
        return commonService.T_Read(securityUtil.getCurrentUserId());
    }
}
