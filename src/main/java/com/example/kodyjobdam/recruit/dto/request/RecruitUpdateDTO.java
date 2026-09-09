package com.example.kodyjobdam.recruit.dto.request;

import com.example.kodyjobdam.recruit.dto.RecruitPeriodDTO;
import lombok.Getter;

@Getter
public class RecruitUpdateDTO {

    private String companyName;

    private RecruitPeriodDTO documentPeriod;

    private RecruitPeriodDTO writtenExamPeriod;

    private RecruitPeriodDTO practicalExamPeriod;

    private RecruitPeriodDTO codingTestPeriod;

    private RecruitPeriodDTO interviewPeriod;

    /** 지원 마감일 표기 문자열. documentPeriod를 함께 보내면 그쪽이 우선한다. */
    private String deadline;

    /** 면접 일정 표기 문자열. interviewPeriod를 함께 보내면 그쪽이 우선한다. */
    private String interviewDate;

    private String summary;
}
