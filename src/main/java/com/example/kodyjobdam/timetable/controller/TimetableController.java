package com.example.kodyjobdam.timetable.controller;

import com.example.kodyjobdam.timetable.dto.response.TimetableReadDTO;
import com.example.kodyjobdam.timetable.service.TimetableService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final SecurityUtil securityUtil;

    /** 로그인한 학생의 학급 시간표. 상담 신청 화면에 참고로 보여준다. */
    @GetMapping("/student/timetable")
    public List<TimetableReadDTO> readMyTimetable(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return timetableService.readMyTimetable(securityUtil.getCurrentUserId(), from, to);
    }
}
