package com.example.kodyjobdam.recruit.controller;

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

    @PostMapping(value = "/recruit/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitResponseDTO> analyze(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(recruitService.analyze(image, securityUtil.getCurrentUserId()));
    }

    @GetMapping("/recruit")
    public List<RecruitResponseDTO> list() {
        return recruitService.list(securityUtil.getCurrentUserId());
    }
}
