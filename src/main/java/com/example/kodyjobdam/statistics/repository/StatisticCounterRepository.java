package com.example.kodyjobdam.statistics.repository;

import com.example.kodyjobdam.statistics.entity.StatisticCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StatisticCounterRepository extends JpaRepository<StatisticCounter, Long> {
    List<StatisticCounter> findByMetricDateBetweenAndCategory(LocalDate from, LocalDate to, String category);
}
