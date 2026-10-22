package com.example.kodyjobdam.statistics.service;

import com.example.kodyjobdam.statistics.entity.StatisticCounter;
import com.example.kodyjobdam.statistics.repository.StatisticCounterRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsServiceTest {
    private final StatisticCounterRepository repository = mock(StatisticCounterRepository.class);
    private final StatisticsService service = new StatisticsService(repository, mock(EntityManager.class));
    private final LocalDate date = LocalDate.of(2026, 10, 22);

    @Test
    void apiRowsAreGroupedAcrossDaysAndErrorsAreCounted() {
        when(repository.findByMetricDateBetweenAndCategory(date.minusDays(1), date, "API"))
                .thenReturn(List.of(
                        counter(date.minusDays(1), "GET /student/**", "200", 3, 30),
                        counter(date, "GET /student/**", "200", 2, 40),
                        counter(date, "GET UNMATCHED", "401", 1, 5)));

        var result = service.api(date.minusDays(1), date);

        assertThat(result.totalCalls()).isEqualTo(6);
        assertThat(result.errorCalls()).isEqualTo(1);
        assertThat(result.daily()).hasSize(2);
        assertThat(result.endpoints()).contains(
                new com.example.kodyjobdam.statistics.dto.ApiStatisticsResponse.Endpoint(
                        "GET", "/student/**", 200, 5, 14.0));
    }

    @Test
    void discordAttemptsIncludeSuccessAndFailure() {
        when(repository.findByMetricDateBetweenAndCategory(date, date, "DISCORD"))
                .thenReturn(List.of(counter(date, "DIRECT_MESSAGE", "SUCCESS", 4, 0),
                        counter(date, "DIRECT_MESSAGE", "FAILURE", 2, 0)));

        var result = service.discord(date, date);

        assertThat(result.attempts()).isEqualTo(6);
        assertThat(result.successes()).isEqualTo(4);
        assertThat(result.failures()).isEqualTo(2);
        assertThat(result.deliveries()).hasSize(1);
    }

    private StatisticCounter counter(LocalDate day, String dimension, String outcome,
                                     long calls, long duration) {
        StatisticCounter row = new StatisticCounter();
        ReflectionTestUtils.setField(row, "metricDate", day);
        ReflectionTestUtils.setField(row, "dimension", dimension);
        ReflectionTestUtils.setField(row, "outcome", outcome);
        ReflectionTestUtils.setField(row, "callCount", calls);
        ReflectionTestUtils.setField(row, "totalDurationMs", duration);
        return row;
    }
}
