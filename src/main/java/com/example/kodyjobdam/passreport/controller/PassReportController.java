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

    /** 면접 합격을 진로 선생님들에게 알린다. 학번·이름은 로그인한 학생 정보로 채운다. */
    @PostMapping("/student/pass-report")
    public ResponseEntity<PassReportResponseDTO> create(@Valid @RequestBody PassReportCreateDTO dto) {
        return ResponseEntity.ok(passReportService.create(dto, securityUtil.getCurrentUserId()));
    }

    /** 내가 보낸 면접 합격 보고 */
    @GetMapping("/student/pass-report")
    public List<PassReportResponseDTO> readMine() {
        return passReportService.readMine(securityUtil.getCurrentUserId());
    }

    // ===== 선생님(TEACHER) 전용 =====

    /** 면접 합격 보고를 한 학생 전체. 학번순으로 내려준다. */
    @GetMapping("/teacher/pass-report")
    public List<PassReportResponseDTO> readAll() {
        return passReportService.readAll();
    }

    /** 면접 합격 보고 한 건. 알림을 누르면 여기로 온다. */
    @GetMapping("/teacher/pass-report/{id}")
    public ResponseEntity<PassReportResponseDTO> readOne(@PathVariable Long id) {
        return ResponseEntity.ok(passReportService.readOne(id));
    }
}
