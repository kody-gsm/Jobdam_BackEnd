package com.example.kodyjobdam.statistics.dto;

import java.util.Map;

public record ServiceStatisticsResponse(Map<String, Long> totals,
                                        Map<String, Map<String, Long>> breakdown) {}
