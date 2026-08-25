package com.example.kodyjobdam.form.controller;

import com.example.kodyjobdam.form.dto.request.FormCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormSubmitDTO;
import com.example.kodyjobdam.form.dto.request.FormUpdateDTO;
import com.example.kodyjobdam.form.dto.response.FormResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionSummaryResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSummaryResponseDTO;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.form.service.FormSubmissionService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    private final FormSubmissionService formSubmissionService;

    private final SecurityUtil securityUtil;

    // ===== 선생님(TEACHER) 전용 =====

    /** 폼 생성 (초안) */
    @PostMapping("/teacher/form")
    public ResponseEntity<FormResponseDTO> create(@Valid @RequestBody FormCreateDTO dto) {
        return ResponseEntity.ok(formService.create(dto, securityUtil.getCurrentUserId()));
    }

    /** 폼 수정 (초안 상태에서만 가능, 질문은 통째로 교체) */
    @PatchMapping("/teacher/form/{id}")
    public ResponseEntity<FormResponseDTO> update(@PathVariable Long id,
                                                  @Valid @RequestBody FormUpdateDTO dto) {
        return ResponseEntity.ok(formService.update(id, dto));
    }

    /** 학생에게 공개 */
    @PostMapping("/teacher/form/{id}/publish")
    public ResponseEntity<FormResponseDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(formService.publish(id));
    }

    /** 응답 마감 */
    @PostMapping("/teacher/form/{id}/close")
    public ResponseEntity<FormResponseDTO> close(@PathVariable Long id) {
        return ResponseEntity.ok(formService.close(id));
    }

    /** 선생님 관리용 전체 목록 (초안 포함) */
    @GetMapping("/teacher/form")
    public List<FormSummaryResponseDTO> listForTeacher() {
        return formService.listForTeacher();
    }

    /** 선생님 관리용 폼 단건 (상태 무관) */
    @GetMapping("/teacher/form/{id}")
    public ResponseEntity<FormResponseDTO> getForTeacher(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getForTeacher(id));
    }

    /** 폼별 제출 목록 */
    @GetMapping("/teacher/form/{id}/submission")
    public List<FormSubmissionSummaryResponseDTO> listSubmissions(@PathVariable Long id) {
        return formSubmissionService.listSubmissions(id);
    }

    /** 제출 단건 상세 */
    @GetMapping("/teacher/form/{id}/submission/{submissionId}")
    public ResponseEntity<FormSubmissionResponseDTO> getSubmission(@PathVariable Long id,
                                                                   @PathVariable Long submissionId) {
        return ResponseEntity.ok(formSubmissionService.getSubmission(id, submissionId));
    }

    // ===== 학생(STUDENT) 전용 =====

    /** 응답 제출 (1인 1회) */
    @PostMapping("/student/form/{id}/submission")
    public ResponseEntity<FormSubmissionResponseDTO> submit(@PathVariable Long id,
                                                            @Valid @RequestBody FormSubmitDTO dto) {
        return ResponseEntity.ok(formSubmissionService.submit(id, dto, securityUtil.getCurrentUserId()));
    }

    /** 내가 제출한 응답 조회 */
    @GetMapping("/student/form/{id}/submission")
    public ResponseEntity<FormSubmissionResponseDTO> getMySubmission(@PathVariable Long id) {
        return ResponseEntity.ok(formSubmissionService.getMySubmission(id, securityUtil.getCurrentUserId()));
    }

    // ===== 공개(로그인 사용자) =====

    /** 공개된 폼 목록 */
    @GetMapping("/form")
    public List<FormSummaryResponseDTO> list() {
        return formService.listPublished();
    }

    /** 공개된 폼 단건 (질문·선택지 포함) */
    @GetMapping("/form/{id}")
    public ResponseEntity<FormResponseDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getPublished(id));
    }
}
