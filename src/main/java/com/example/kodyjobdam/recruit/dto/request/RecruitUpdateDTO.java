package com.example.kodyjobdam.recruit.dto.request;

import com.example.kodyjobdam.recruit.dto.RecruitPeriodDTO;
import com.example.kodyjobdam.recruit.entity.RecruitField;
import lombok.Getter;

import java.util.List;

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

    /** 이 공고가 뽑는 직무 분야. 생략하면 기존 값을 그대로 두고, 빈 배열을 보내면 비운다. */
    private List<RecruitField> fields;

    private String summary;
}
