package com.example.kodyjobdam.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record DiscordStatisticsResponse(LocalDate from, LocalDate to, long attempts, long successes,
                                        long failures, List<Daily> daily, List<Delivery> deliveries) {
    public record Daily(LocalDate date, long attempts, long successes, long failures) {}
    public record Delivery(String type, long successes, long failures) {}
}
