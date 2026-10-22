package com.example.kodyjobdam.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticRecorder {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbcTemplate;

    public void recordApi(String method, String route, int status, long durationMs) {
        String dimension = method + " " + route;
        record("API", dimension.length() > 240 ? dimension.substring(0, 240) : dimension,
                Integer.toString(status), durationMs);
    }

    public void recordDiscord(String type, boolean success) {
        record("DISCORD", type, success ? "SUCCESS" : "FAILURE", 0);
    }

    private void record(String category, String dimension, String outcome, long durationMs) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO statistic_counter
                        (metric_date, category, dimension, outcome, call_count, total_duration_ms)
                    VALUES (?, ?, ?, ?, 1, ?)
                    ON DUPLICATE KEY UPDATE
                        call_count = call_count + 1,
                        total_duration_ms = total_duration_ms + VALUES(total_duration_ms)
                    """, LocalDate.now(SEOUL), category, dimension, outcome, durationMs);
        } catch (RuntimeException e) {
            // 통계 저장 장애가 실제 API 응답이나 Discord 발송 결과를 바꾸지 않게 한다.
            log.warn("통계 저장 실패: category={}, dimension={}", category, dimension, e);
        }
    }
}
