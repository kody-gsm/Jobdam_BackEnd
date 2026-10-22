package com.example.kodyjobdam.statistics.service;

import com.example.kodyjobdam.statistics.dto.ApiStatisticsResponse;
import com.example.kodyjobdam.statistics.dto.DiscordStatisticsResponse;
import com.example.kodyjobdam.statistics.dto.ServiceStatisticsResponse;
import com.example.kodyjobdam.statistics.entity.StatisticCounter;
import com.example.kodyjobdam.statistics.repository.StatisticCounterRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final StatisticCounterRepository repository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public ApiStatisticsResponse api(LocalDate from, LocalDate to) {
        List<StatisticCounter> rows = repository.findByMetricDateBetweenAndCategory(from, to, "API");
        Map<LocalDate, long[]> days = new TreeMap<>();
        Map<String, long[]> endpointTotals = new LinkedHashMap<>();
        long total = 0, errors = 0, duration = 0;
        for (StatisticCounter row : rows) {
            long calls = row.getCallCount();
            int status = Integer.parseInt(row.getOutcome());
            boolean error = status >= 400;
            total += calls;
            duration += row.getTotalDurationMs();
            if (error) errors += calls;
            long[] day = days.computeIfAbsent(row.getMetricDate(), ignored -> new long[2]);
            day[0] += calls;
            if (error) day[1] += calls;
            int split = row.getDimension().indexOf(' ');
            String method = split < 0 ? row.getDimension() : row.getDimension().substring(0, split);
            String route = split < 0 ? "UNMATCHED" : row.getDimension().substring(split + 1);
            long[] endpoint = endpointTotals.computeIfAbsent(method + " " + route + "\u0000" + status,
                    ignored -> new long[2]);
            endpoint[0] += calls;
            endpoint[1] += row.getTotalDurationMs();
        }
        List<ApiStatisticsResponse.Endpoint> endpoints = new ArrayList<>();
        endpointTotals.forEach((key, values) -> {
            int separator = key.lastIndexOf('\u0000');
            String methodAndRoute = key.substring(0, separator);
            int split = methodAndRoute.indexOf(' ');
            endpoints.add(new ApiStatisticsResponse.Endpoint(methodAndRoute.substring(0, split),
                    methodAndRoute.substring(split + 1), Integer.parseInt(key.substring(separator + 1)),
                    values[0], average(values[1], values[0])));
        });
        endpoints.sort(Comparator.comparingLong(ApiStatisticsResponse.Endpoint::calls).reversed());
        List<ApiStatisticsResponse.Daily> daily = days.entrySet().stream()
                .map(entry -> new ApiStatisticsResponse.Daily(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
        return new ApiStatisticsResponse(from, to, total, errors, average(duration, total), daily, endpoints);
    }

    @Transactional(readOnly = true)
    public DiscordStatisticsResponse discord(LocalDate from, LocalDate to) {
        List<StatisticCounter> rows = repository.findByMetricDateBetweenAndCategory(from, to, "DISCORD");
        Map<LocalDate, long[]> days = new TreeMap<>();
        Map<String, long[]> types = new TreeMap<>();
        long success = 0, failure = 0;
        for (StatisticCounter row : rows) {
            boolean sent = "SUCCESS".equals(row.getOutcome());
            long calls = row.getCallCount();
            if (sent) success += calls;
            else failure += calls;
            long[] day = days.computeIfAbsent(row.getMetricDate(), ignored -> new long[2]);
            long[] type = types.computeIfAbsent(row.getDimension(), ignored -> new long[2]);
            day[sent ? 0 : 1] += calls;
            type[sent ? 0 : 1] += calls;
        }
        List<DiscordStatisticsResponse.Daily> daily = days.entrySet().stream()
                .map(entry -> new DiscordStatisticsResponse.Daily(entry.getKey(),
                        entry.getValue()[0] + entry.getValue()[1], entry.getValue()[0], entry.getValue()[1]))
                .toList();
        List<DiscordStatisticsResponse.Delivery> deliveries = types.entrySet().stream()
                .map(entry -> new DiscordStatisticsResponse.Delivery(entry.getKey(),
                        entry.getValue()[0], entry.getValue()[1]))
                .toList();
        return new DiscordStatisticsResponse(from, to, success + failure, success, failure, daily, deliveries);
    }

    @Transactional(readOnly = true)
    public ServiceStatisticsResponse serviceData() {
        Map<String, Long> totals = new LinkedHashMap<>();
        Map<String, Map<String, Long>> statuses = new LinkedHashMap<>();
        add(totals, statuses, "users", "User", "role");
        add(totals, statuses, "commonReservations", "CommonEntity", "state");
        add(totals, statuses, "courseReservations", "CourseEntity", "state");
        add(totals, statuses, "recruits", "RecruitEntity", "status");
        add(totals, statuses, "forms", "FormEntity", "status");
        add(totals, statuses, "formSubmissions", "FormSubmissionEntity", "status");
        add(totals, statuses, "notifications", "Notification", "type");
        add(totals, statuses, "counselingReminders", "CounselingReminder", "reservationKind");
        add(totals, statuses, "banners", "BannerEntity", "active");
        add(totals, statuses, "passReports", "PassReportEntity", "type");
        addCount(totals, "formQuestions", "FormQuestionEntity");
        addCount(totals, "formQuestionOptions", "FormQuestionOptionEntity");
        addCount(totals, "formAnswers", "FormAnswerEntity");
        addCount(totals, "formAnswerOptions", "FormAnswerOptionEntity");
        addCount(totals, "formFiles", "FormFileEntity");
        addCount(totals, "commonWeeklyLocks", "CommonWeeklyLockEntity");
        addCount(totals, "courseWeeklyLocks", "CourseWeeklyLockEntity");
        addCount(totals, "emailVerificationCodes", "EmailVerificationCode");
        return new ServiceStatisticsResponse(totals, statuses);
    }

    private void addCount(Map<String, Long> totals, String key, String entity) {
        totals.put(key, entityManager.createQuery("select count(e) from " + entity + " e", Long.class)
                .getSingleResult());
    }

    private void add(Map<String, Long> totals, Map<String, Map<String, Long>> statuses,
                     String key, String entity, String field) {
        List<Object[]> rows = entityManager.createQuery(
                "select e." + field + ", count(e) from " + entity + " e group by e." + field, Object[].class)
                .getResultList();
        Map<String, Long> breakdown = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : rows) {
            long count = (Long) row[1];
            breakdown.put(row[0] == null ? "UNSPECIFIED" : row[0].toString(), count);
            total += count;
        }
        totals.put(key, total);
        statuses.put(key, breakdown);
    }

    public LocalDate defaultFrom() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(29);
    }

    public LocalDate defaultTo() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private double average(long duration, long calls) {
        return calls == 0 ? 0 : Math.round((double) duration / calls * 100.0) / 100.0;
    }
}
