package com.example.kodyjobdam.recruit.controller;

import com.example.kodyjobdam.recruit.dto.request.RecruitCreateDTO;
import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.service.RecruitService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import jakarta.validation.Valid;
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

    /**
     * 이미지 분석 → 초안 저장 후 결과 반환.
     * 이미지는 같은 이름(image)으로 여러 장 보낼 수 있다. createForm을 false로 보내면 지원 폼 없이 만든다.
     */
    @PostMapping(value = "/teacher/recruit/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitResponseDTO> analyze(
            @RequestParam("image") List<MultipartFile> images,
            @RequestParam(value = "createForm", defaultValue = "true") boolean createForm) {
        return ResponseEntity.ok(recruitService.analyze(images, createForm, securityUtil.getCurrentUserId()));
    }

    /** 이미지 없이 직접 입력해 초안 저장 */
    @PostMapping("/teacher/recruit")
    public ResponseEntity<RecruitResponseDTO> create(@Valid @RequestBody RecruitCreateDTO dto) {
        return ResponseEntity.ok(recruitService.create(dto, securityUtil.getCurrentUserId()));
    }

    /** 분석 결과 수정 */
    @PatchMapping("/teacher/recruit/{id}")
    public ResponseEntity<RecruitResponseDTO> update(@PathVariable Long id,
                                                     @RequestBody RecruitUpdateDTO dto) {
        return ResponseEntity.ok(recruitService.update(id, dto, securityUtil.getCurrentUserId()));
    }

    /** 채용 공고 삭제 */
    @DeleteMapping("/teacher/recruit/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recruitService.delete(id, securityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /** 공고 이미지 추가. 같은 이름(image)으로 여러 장 보낼 수 있다. */
    @PostMapping(value = "/teacher/recruit/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitResponseDTO> addImages(@PathVariable Long id,
                                                        @RequestParam("image") List<MultipartFile> images) {
        return ResponseEntity.ok(recruitService.addImages(id, images, securityUtil.getCurrentUserId()));
    }

    /** 공고 이미지 한 장 삭제 */
    @DeleteMapping("/teacher/recruit/{id}/image/{imageId}")
    public ResponseEntity<RecruitResponseDTO> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(recruitService.deleteImage(id, imageId, securityUtil.getCurrentUserId()));
    }

    /** 학생에게 공개 */
    @PostMapping("/teacher/recruit/{id}/publish")
    public ResponseEntity<RecruitResponseDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(recruitService.publish(id, securityUtil.getCurrentUserId()));
    }

    /** 선생님 관리용 전체 목록 (초안 포함) */
    @GetMapping("/teacher/recruit")
    public List<RecruitResponseDTO> listForTeacher() {
        return recruitService.listForTeacher(securityUtil.getCurrentUserId());
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
