package com.example.kodyjobdam.common.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * 상담을 잡을 수 있는 교시와 그 시작 시각.
 *
 * <p>교시는 클라이언트가 보내는 라벨("3교시", "점심시간") 그대로 저장되므로,
 * 시작 시각이 필요한 곳에서는 이 표로 라벨을 되짚는다.</p>
 */
public enum CounselingPeriod {

    FIRST("1교시", LocalTime.of(8, 40)),
    SECOND("2교시", LocalTime.of(9, 40)),
    THIRD("3교시", LocalTime.of(10, 40)),
    FOURTH("4교시", LocalTime.of(11, 40)),
    LUNCH("점심시간", LocalTime.of(12, 30)),
    FIFTH("5교시", LocalTime.of(13, 30)),
    SIXTH("6교시", LocalTime.of(14, 30)),
    SEVENTH("7교시", LocalTime.of(15, 30)),
    EIGHTH("8교시", LocalTime.of(16, 40)),
    NINTH("9교시", LocalTime.of(17, 40)),
    DINNER("저녁시간", LocalTime.of(18, 30));

    private final String label;

    private final LocalTime startTime;

    CounselingPeriod(String label, LocalTime startTime) {
        this.label = label;
        this.startTime = startTime;
    }

    /** 표에 없는 라벨이면 비어 있는 값을 돌려준다. 시작 시각을 모르는 교시라는 뜻이다. */
    public static Optional<CounselingPeriod> from(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }

        String trimmed = label.trim();
        return Arrays.stream(values())
                .filter(period -> period.label.equals(trimmed))
                .findFirst();
    }

    public String getLabel() {
        return label;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalDateTime startsAt(LocalDate date) {
        return date.atTime(startTime);
    }
}
