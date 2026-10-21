package com.example.kodyjobdam.passreport.controller;

import com.example.kodyjobdam.passreport.dto.request.PassReportCreateDTO;
import com.example.kodyjobdam.passreport.dto.response.PassReportResponseDTO;
import com.example.kodyjobdam.passreport.service.PassReportService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PassReportController {

    private final PassReportService passReportService;

    private final SecurityUtil securityUtil;

    // ===== 학생(STUDENT) 전용 =====

    /** 서류·면접 합격을 진로 선생님에게 알린다. 받는 선생님은 /student/course/teachers 목록에서 고른다. */
    @PostMapping("/student/pass-report")
    public ResponseEntity<PassReportResponseDTO> create(@Valid @RequestBody PassReportCreateDTO dto) {
        return ResponseEntity.ok(passReportService.create(dto, securityUtil.getCurrentUserId()));
    }

    /** 내가 보낸 합격 보고 */
    @GetMapping("/student/pass-report")
    public List<PassReportResponseDTO> readMine() {
        return passReportService.readMine(securityUtil.getCurrentUserId());
    }

    // ===== 선생님(TEACHER) 전용 =====

    /** 내가 받은 합격 보고 */
    @GetMapping("/teacher/pass-report")
    public List<PassReportResponseDTO> readReceived() {
        return passReportService.readReceived(securityUtil.getCurrentUserId());
    }

    /** 내가 받은 합격 보고 한 건. 알림을 누르면 여기로 온다. */
    @GetMapping("/teacher/pass-report/{id}")
    public ResponseEntity<PassReportResponseDTO> readReceivedOne(@PathVariable Long id) {
        return ResponseEntity.ok(passReportService.readReceivedOne(id, securityUtil.getCurrentUserId()));
    }
}
