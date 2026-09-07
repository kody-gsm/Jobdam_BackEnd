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

    private LocalDate startDate;

    private LocalDate endDate;
}
