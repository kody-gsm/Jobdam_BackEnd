package com.example.kodyjobdam.banner.controller;

import com.example.kodyjobdam.banner.dto.response.BannerResponseDTO;
import com.example.kodyjobdam.banner.service.BannerService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    private final SecurityUtil securityUtil;

    @PostMapping(value = "/teacher/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerResponseDTO> saveCurrent(@RequestParam("image") MultipartFile image,
                                                         @RequestParam(required = false) String title,
                                                         @RequestParam(required = false) String content,
                                                         @RequestParam(required = false) String link) {
        return ResponseEntity.ok(bannerService.saveCurrent(
                image,
                title,
                content,
                link,
                securityUtil.getCurrentUserId()
        ));
    }

    @GetMapping("/teacher/banner")
    public ResponseEntity<BannerResponseDTO> getCurrentForTeacher() {
        return bannerService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/student/banner")
    public ResponseEntity<BannerResponseDTO> getCurrentForStudent() {
        return bannerService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
