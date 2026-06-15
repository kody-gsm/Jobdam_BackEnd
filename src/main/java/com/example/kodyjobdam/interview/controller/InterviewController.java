package com.example.kodyjobdam.interview.controller;

import com.example.kodyjobdam.interview.dto.InterviewRequest;
import com.example.kodyjobdam.interview.dto.InterviewResponse;
import com.example.kodyjobdam.interview.service.InterviewService;
import com.example.kodyjobdam.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<List<InterviewResponse>> createReservation(
            @Valid @RequestBody InterviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<InterviewResponse> responses = interviewService.createReservation(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PatchMapping("/cancel/{reservation_id}")
    public ResponseEntity<String> cancelReservation(
            @PathVariable("reservation_id") Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        interviewService.cancelReservation(reservationId, userDetails.getUsername());
        return ResponseEntity.ok("예약이 성공적으로 취소되었습니다.");
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<InterviewResponse> responses = interviewService.getMyReservations(userDetails.getUsername());
        return ResponseEntity.ok(responses);
    }
}