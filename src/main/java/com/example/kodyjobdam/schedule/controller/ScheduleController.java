package com.example.kodyjobdam.schedule.controller;

import com.example.kodyjobdam.schedule.dto.response.ScheduleReadDTO;
import com.example.kodyjobdam.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleReadDTO> readSchedules(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(required = false) Integer grade) {
        return scheduleService.readSchedules(from, to, grade);
    }

    @GetMapping("/monthly")
    public List<ScheduleReadDTO> readMonthlySchedules(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Integer grade) {
        return scheduleService.readMonthlySchedules(year, month, grade);
    }
}
