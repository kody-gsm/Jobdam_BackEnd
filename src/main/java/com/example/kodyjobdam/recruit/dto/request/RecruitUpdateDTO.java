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

    private String summary;
}
