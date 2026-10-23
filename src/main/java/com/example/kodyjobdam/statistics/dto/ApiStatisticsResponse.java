package com.example.kodyjobdam.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record ApiStatisticsResponse(LocalDate from, LocalDate to, long totalCalls, long errorCalls,
                                    double averageDurationMs, List<Daily> daily, List<Endpoint> endpoints) {
    public record Daily(LocalDate date, long calls, long errorCalls) {}
    public record Endpoint(String method, String route, int status, long calls, double averageDurationMs) {}
}
