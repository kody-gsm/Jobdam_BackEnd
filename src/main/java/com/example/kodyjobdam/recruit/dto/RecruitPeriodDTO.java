package com.example.kodyjobdam.recruit.dto;

import com.example.kodyjobdam.recruit.entity.RecruitPeriod;

import java.time.LocalDate;

/** 전형 기간 요청/응답 표현 */
public record RecruitPeriodDTO(
        LocalDate startDate,
        LocalDate endDate
) {

    public static RecruitPeriodDTO from(RecruitPeriod period) {
        return period == null ? null : new RecruitPeriodDTO(period.getStartDate(), period.getEndDate());
    }

    public static RecruitPeriod toPeriod(RecruitPeriodDTO dto) {
        return dto == null ? null : new RecruitPeriod(dto.startDate(), dto.endDate());
    }
}
