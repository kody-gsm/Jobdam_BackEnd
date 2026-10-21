package com.example.kodyjobdam.user.controller;

import com.example.kodyjobdam.user.dto.TutorialStatusResponse;
import com.example.kodyjobdam.user.security.SecurityUtil;
import com.example.kodyjobdam.user.service.TutorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/tutorial")
public class TutorialController {

    private final TutorialService tutorialService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public TutorialStatusResponse getStatus() {
        return tutorialService.getStatus(securityUtil.getCurrentUserId());
    }

    @PatchMapping("/complete")
    public TutorialStatusResponse complete() {
        return tutorialService.complete(securityUtil.getCurrentUserId());
    }
}
