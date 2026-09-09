package com.example.kodyjobdam.recruit.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}
