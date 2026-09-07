package com.example.kodyjobdam.schedule.service;

import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.schedule.client.NeisScheduleClient;
import com.example.kodyjobdam.schedule.client.NeisScheduleClient.NeisScheduleRow;
import com.example.kodyjobdam.schedule.dto.response.ScheduleReadDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String EVENT_APPLIED = "Y";
    private static final int MAX_RANGE_DAYS = 366;

    private final NeisScheduleClient neisScheduleClient;

    /** 나이스 호출 횟수를 줄이기 위한 조회 구간 단위 캐시 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${neis.cache.ttl-minutes:60}")
    private long cacheTtlMinutes;

    public List<ScheduleReadDTO> readSchedules(LocalDate from, LocalDate to, Integer grade) {
        if (from == null || to == null) {
            throw ScheduleException.badRequest("조회 시작일과 종료일을 입력해주세요.");
        }
        if (from.isAfter(to)) {
            throw ScheduleException.badRequest("조회 시작일이 종료일보다 늦을 수 없습니다.");
        }
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() > MAX_RANGE_DAYS) {
            throw ScheduleException.badRequest("한 번에 조회할 수 있는 기간은 최대 1년입니다.");
        }
        if (grade != null && (grade < 1 || grade > 3)) {
            throw ScheduleException.badRequest("학년은 1에서 3 사이여야 합니다.");
        }

        return findCached(from, to).stream()
                .filter(schedule -> grade == null || schedule.getGrades().contains(grade))
                .toList();
    }

    /**
     * 해당 날짜가 휴업일(공휴일, 토요휴업일, 학교장재량휴업일 등)인지 판단한다.
     *
     * <p>예약 차단용 보조 판정이므로 나이스 조회에 실패하면 예약을 막지 않도록 false를 돌려준다.
     * 나이스 장애가 예약 기능 전체를 멈추게 해서는 안 된다.</p>
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }

        YearMonth yearMonth = YearMonth.from(date);
        try {
            return findCached(yearMonth.atDay(1), yearMonth.atEndOfMonth()).stream()
                    .anyMatch(schedule -> date.equals(schedule.getDate()) && schedule.isHoliday());
        } catch (RuntimeException e) {
            log.warn("휴업일 판단에 실패해 예약을 허용합니다. date={}", date, e);
            return false;
        }
    }

    public List<ScheduleReadDTO> readMonthlySchedules(int year, int month, Integer grade) {
        if (month < 1 || month > 12) {
            throw ScheduleException.badRequest("월은 1에서 12 사이여야 합니다.");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        return readSchedules(yearMonth.atDay(1), yearMonth.atEndOfMonth(), grade);
    }

    private List<ScheduleReadDTO> findCached(LocalDate from, LocalDate to) {
        String key = from + "~" + to;
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh(cacheTtlMinutes)) {
            return cached.schedules();
        }

        List<ScheduleReadDTO> schedules = neisScheduleClient.fetchSchedules(from, to).stream()
                .map(this::toDTO)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ScheduleReadDTO::getDate))
                .toList();

        cache.put(key, new CacheEntry(schedules, Instant.now()));
        return schedules;
    }

    private ScheduleReadDTO toDTO(NeisScheduleRow row) {
        LocalDate date = parseDate(row.date());
        if (date == null) {
            return null;
        }

        return new ScheduleReadDTO(
                date,
                emptyToNull(row.eventName()),
                emptyToNull(row.eventContent()),
                emptyToNull(row.holidayType()),
                toGrades(row)
        );
    }

    private List<Integer> toGrades(NeisScheduleRow row) {
        List<Integer> grades = new ArrayList<>();
        if (EVENT_APPLIED.equals(row.firstGrade())) {
            grades.add(1);
        }
        if (EVENT_APPLIED.equals(row.secondGrade())) {
            grades.add(2);
        }
        if (EVENT_APPLIED.equals(row.thirdGrade())) {
            grades.add(3);
        }
        return grades;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim(), YMD);
        } catch (DateTimeParseException e) {
            log.warn("나이스 학사일정 날짜 형식이 올바르지 않습니다. value={}", value);
            return null;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CacheEntry(List<ScheduleReadDTO> schedules, Instant cachedAt) {

        private boolean isFresh(long ttlMinutes) {
            return Duration.between(cachedAt, Instant.now()).toMinutes() < ttlMinutes;
        }
    }
}
