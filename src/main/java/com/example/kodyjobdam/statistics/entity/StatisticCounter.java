package com.example.kodyjobdam.statistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "statistic_counter", uniqueConstraints = @UniqueConstraint(
        name = "uk_statistic_counter", columnNames = {"metric_date", "category", "dimension", "outcome"}))
public class StatisticCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(nullable = false, length = 16)
    private String category;

    @Column(nullable = false, length = 240)
    private String dimension;

    @Column(nullable = false, length = 16)
    private String outcome;

    @Column(name = "call_count", nullable = false)
    private long callCount;

    @Column(name = "total_duration_ms", nullable = false)
    private long totalDurationMs;
}
