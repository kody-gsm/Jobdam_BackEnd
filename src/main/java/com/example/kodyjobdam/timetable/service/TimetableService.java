package com.example.kodyjobdam.timetable.service;

import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.exception.ReservationException;
import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.timetable.client.NeisTimetableClient;
import com.example.kodyjobdam.timetable.client.NeisTimetableClient.NeisTimetableRow;
import com.example.kodyjobdam.timetable.dto.response.TimetableReadDTO;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 학생 본인이 속한 학급의 시간표를 읽는다.
 *
 * <p>상담 신청 화면에 "그 교시에 무슨 수업이 있는지"를 참고로 보여주기 위한 것이다.
 * 예약을 막는 근거로는 쓰지 않는다. 수업을 빼고 상담을 갈지는 학교가 정할 일이고,
 * 잠그는 일은 선생님의 잠금 기능이 맡는다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_RANGE_DAYS = 31;

    private final NeisTimetableClient neisTimetableClient;
    private final UserRepository userRepository;

    /** 나이스 호출 횟수를 줄이기 위한 학급·기간 단위 캐시 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${neis.cache.ttl-minutes:60}")
    private long cacheTtlMinutes;

    /**
     * 로그인한 학생의 학급 시간표.
     *
     * <p>학년·반을 모르는 사용자(선생님, 아직 동기화되지 않은 학생)는 빈 목록을 받는다.
     * 시간표는 참고 정보라 없다고 해서 화면을 막을 이유가 없다.</p>
     */
    public List<TimetableReadDTO> readMyTimetable(Long userId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ScheduleException.badRequest("조회 기간을 선택해주세요.");
        }
        if (from.isAfter(to)) {
            throw ScheduleException.badRequest("시작일이 종료일보다 늦습니다.");
        }
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() > MAX_RANGE_DAYS) {
            throw ScheduleException.badRequest("시간표는 최대 " + MAX_RANGE_DAYS + "일까지 조회할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ReservationException.notFound("회원이 없습니다."));
        if (user.getGrade() == null || user.getClassNum() == null) {
            return List.of();
        }

        return findCached(user.getGrade(), user.getClassNum(), from, to);
    }

    private List<TimetableReadDTO> findCached(int grade, int classNum, LocalDate from, LocalDate to) {
        String key = grade + "-" + classNum + "/" + from + "~" + to;
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh(cacheTtlMinutes)) {
            return cached.timetables();
        }

        List<TimetableReadDTO> timetables = neisTimetableClient.fetchTimetables(grade, classNum, from, to).stream()
                .map(this::toDTO)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TimetableReadDTO::getDate).thenComparing(TimetableReadDTO::getPeriod))
                .toList();

        cache.put(key, new CacheEntry(timetables, Instant.now()));
        return timetables;
    }

    private TimetableReadDTO toDTO(NeisTimetableRow row) {
        LocalDate date = parseDate(row.date());
        String period = toPeriodLabel(row.period());
        if (date == null || period == null) {
            return null;
        }

        return new TimetableReadDTO(date, period, toSubject(row.subject()), emptyToNull(row.classroom()));
    }

    /**
     * 나이스 교시 번호를 상담 예약이 쓰는 교시 라벨로 바꾼다.
     *
     * <p>상담 시간표에 없는 교시는 화면에 놓을 자리가 없으므로 버린다.</p>
     */
    private String toPeriodLabel(String value) {
        String period = emptyToNull(value);
        if (period == null) {
            return null;
        }

        return CounselingPeriod.from(period + "교시")
                .map(CounselingPeriod::getLabel)
                .orElseGet(() -> {
                    log.warn("상담 시간표에 없는 교시라 건너뜁니다. period={}", period);
                    return null;
                });
    }

    /** 나이스는 실습 과목 앞에 '*'를 붙여 내려준다. 화면에 그대로 보일 필요는 없다. */
    private String toSubject(String value) {
        String subject = emptyToNull(value);
        if (subject == null) {
            return null;
        }

        return emptyToNull(subject.replaceFirst("^\\*+", ""));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim(), YMD);
        } catch (DateTimeParseException e) {
            log.warn("나이스 시간표 날짜 형식이 올바르지 않습니다. value={}", value);
            return null;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CacheEntry(List<TimetableReadDTO> timetables, Instant cachedAt) {

        private boolean isFresh(long ttlMinutes) {
            return Duration.between(cachedAt, Instant.now()).toMinutes() < ttlMinutes;
        }
    }
}
