package com.example.kodyjobdam.recruit.entity;

import com.example.kodyjobdam.common.exception.RecruitException;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 전형 기간(시작일~종료일). 하루짜리 전형은 시작일과 종료일이 같다. */
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class RecruitPeriod {

    private static final String RANGE_SEPARATOR = " ~ ";

    private LocalDate startDate;

    private LocalDate endDate;

    public boolean isEmpty() {
        return startDate == null && endDate == null;
    }

    /** 화면 표기용 문자열. 하루짜리는 "YYYY-MM-DD", 여러 날은 "YYYY-MM-DD ~ YYYY-MM-DD". */
    public String toDisplay() {
        if (isEmpty()) {
            return null;
        }
        if (startDate == null) {
            return endDate.toString();
        }
        if (endDate == null || startDate.equals(endDate)) {
            return startDate.toString();
        }
        return startDate + RANGE_SEPARATOR + endDate;
    }

    /** {@link #toDisplay()} 형식의 문자열을 기간으로 되돌린다. 비어 있으면 null. */
    public static RecruitPeriod parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] tokens = text.split("~");
        if (tokens.length > 2) {
            throw RecruitException.badRequest("기간은 \"YYYY-MM-DD\" 또는 \"YYYY-MM-DD ~ YYYY-MM-DD\" 형식으로 입력해주세요.");
        }

        LocalDate startDate = parseDate(tokens[0]);
        LocalDate endDate = tokens.length == 2 ? parseDate(tokens[1]) : startDate;
        return new RecruitPeriod(startDate, endDate);
    }

    private static LocalDate parseDate(String value) {
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw RecruitException.badRequest("날짜는 YYYY-MM-DD 형식으로 입력해주세요: " + trimmed);
        }
    }
}
