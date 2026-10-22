package com.example.kodyjobdam.statistics.controller;

import com.example.kodyjobdam.statistics.dto.ApiStatisticsResponse;
import com.example.kodyjobdam.statistics.dto.DiscordStatisticsResponse;
import com.example.kodyjobdam.statistics.dto.ServiceStatisticsResponse;
import com.example.kodyjobdam.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/api")
    public ApiStatisticsResponse api(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = range(from, to);
        return service.api(range[0], range[1]);
    }

    @GetMapping("/discord")
    public DiscordStatisticsResponse discord(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = range(from, to);
        return service.discord(range[0], range[1]);
    }

    @GetMapping("/service")
    public ServiceStatisticsResponse serviceData() {
        return service.serviceData();
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? service.defaultFrom() : from;
        LocalDate end = to == null ? service.defaultTo() : to;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간은 1~366일이어야 합니다.");
        }
        return new LocalDate[]{start, end};
    }
}
