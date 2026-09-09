package com.example.kodyjobdam.form.controller;

import com.example.kodyjobdam.form.dto.request.FormCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormSubmitDTO;
import com.example.kodyjobdam.form.dto.request.FormUpdateDTO;
import com.example.kodyjobdam.form.dto.response.FormFileDownloadDTO;
import com.example.kodyjobdam.form.dto.response.FormFileResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionSummaryResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSummaryResponseDTO;
import com.example.kodyjobdam.form.service.FormFileService;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.form.service.FormSubmissionService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    private final FormSubmissionService formSubmissionService;

    private final FormFileService formFileService;

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
        return ResponseEntity.ok(formService.update(id, dto, securityUtil.getCurrentUserId()));
    }

    /** 학생에게 공개 */
    /** 폼 삭제 */
    @DeleteMapping("/teacher/form/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        formService.delete(id, securityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher/form/{id}/publish")
    public ResponseEntity<FormResponseDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(formService.publish(id, securityUtil.getCurrentUserId()));
    }

    /** 응답 마감 */
    @PostMapping("/teacher/form/{id}/close")
    public ResponseEntity<FormResponseDTO> close(@PathVariable Long id) {
        return ResponseEntity.ok(formService.close(id, securityUtil.getCurrentUserId()));
    }

    /** 선생님 관리용 전체 목록 (초안 포함) */
    @GetMapping("/teacher/form")
    public List<FormSummaryResponseDTO> listForTeacher() {
        return formService.listForTeacher(securityUtil.getCurrentUserId());
    }

    /** 선생님 관리용 폼 단건 (상태 무관) */
    @GetMapping("/teacher/form/{id}")
    public ResponseEntity<FormResponseDTO> getForTeacher(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getForTeacher(id, securityUtil.getCurrentUserId()));
    }

    /** 폼별 제출 목록 */
    @GetMapping("/teacher/form/{id}/submission")
    public List<FormSubmissionSummaryResponseDTO> listSubmissions(@PathVariable Long id) {
        return formSubmissionService.listSubmissions(id, securityUtil.getCurrentUserId());
    }

    /** 제출 단건 상세 */
    @GetMapping("/teacher/form/{id}/submission/{submissionId}")
    public ResponseEntity<FormSubmissionResponseDTO> getSubmission(@PathVariable Long id,
                                                                   @PathVariable Long submissionId) {
        return ResponseEntity.ok(formSubmissionService.getSubmission(id, submissionId, securityUtil.getCurrentUserId()));
    }

    /** 지원자 확정 */
    @PostMapping("/teacher/form/{id}/submission/{submissionId}/confirm")
    public ResponseEntity<FormSubmissionResponseDTO> confirmSubmission(@PathVariable Long id,
                                                                       @PathVariable Long submissionId) {
        return ResponseEntity.ok(
                formSubmissionService.confirm(id, submissionId, securityUtil.getCurrentUserId()));
    }

    /** 지원자 확정 되돌리기 */
    @DeleteMapping("/teacher/form/{id}/submission/{submissionId}/confirm")
    public ResponseEntity<FormSubmissionResponseDTO> cancelConfirmSubmission(@PathVariable Long id,
                                                                             @PathVariable Long submissionId) {
        return ResponseEntity.ok(
                formSubmissionService.cancelConfirm(id, submissionId, securityUtil.getCurrentUserId()));
    }

    // ===== 학생(STUDENT) 전용 =====

    /** 응답 제출 (1인 1회) */
    @PostMapping("/student/form/{id}/submission")
    public ResponseEntity<FormSubmissionResponseDTO> submit(@PathVariable Long id,
                                                            @Valid @RequestBody FormSubmitDTO dto) {
        return ResponseEntity.ok(formSubmissionService.submit(id, dto, securityUtil.getCurrentUserId()));
    }

    /** 내가 제출한 응답 수정 (재응답) */
    @PatchMapping("/student/form/{id}/submission")
    public ResponseEntity<FormSubmissionResponseDTO> resubmit(@PathVariable Long id,
                                                              @Valid @RequestBody FormSubmitDTO dto) {
        return ResponseEntity.ok(formSubmissionService.resubmit(id, dto, securityUtil.getCurrentUserId()));
    }

    /** 답변에 붙일 파일 업로드. 돌려받은 id를 답변의 fileId로 보내 제출한다. */
    @PostMapping(value = "/student/form/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FormFileResponseDTO> uploadFile(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(formFileService.upload(id, file, securityUtil.getCurrentUserId()));
    }

    /** 내가 제출한 응답 조회 */
    @GetMapping("/student/form/{id}/submission")
    public ResponseEntity<FormSubmissionResponseDTO> getMySubmission(@PathVariable Long id) {
        return ResponseEntity.ok(formSubmissionService.getMySubmission(id, securityUtil.getCurrentUserId()));
    }

    // ===== 공개(로그인 사용자) =====

    /** 첨부 파일 내려받기 (올린 학생 본인과 폼을 만든 선생님만) */
    @GetMapping("/form/file/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FormFileDownloadDTO download = formFileService.download(fileId, securityUtil.getCurrentUserId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .body(download.resource());
    }

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
