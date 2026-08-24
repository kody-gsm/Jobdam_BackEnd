package com.example.kodyjobdam.recruit.controller;

import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.service.RecruitService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;

    private final SecurityUtil securityUtil;

    // ===== 선생님(TEACHER) 전용 =====

    /** 이미지 분석 → 초안 저장 후 결과 반환 */
    @PostMapping(value = "/teacher/recruit/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitResponseDTO> analyze(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(recruitService.analyze(image, securityUtil.getCurrentUserId()));
    }

    /** 분석 결과 수정 */
    @PatchMapping("/teacher/recruit/{id}")
    public ResponseEntity<RecruitResponseDTO> update(@PathVariable Long id,
                                                     @RequestBody RecruitUpdateDTO dto) {
        return ResponseEntity.ok(recruitService.update(id, dto));
    }

    /** 학생에게 공개 */
    @PostMapping("/teacher/recruit/{id}/publish")
    public ResponseEntity<RecruitResponseDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(recruitService.publish(id));
    }

    /** 선생님 관리용 전체 목록 (초안 포함) */
    @GetMapping("/teacher/recruit")
    public List<RecruitResponseDTO> listForTeacher() {
        return recruitService.listForTeacher();
    }

    // ===== 학생/공개용 =====

    /** 공개된 공고 목록 */
    @GetMapping("/recruit")
    public List<RecruitResponseDTO> list() {
        return recruitService.listPublished();
    }

    /** 공개된 공고 단건 */
    @GetMapping("/recruit/{id}")
    public ResponseEntity<RecruitResponseDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(recruitService.getPublished(id));
    }
}
