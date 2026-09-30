package com.example.kodyjobdam.recruit.client;

import com.example.kodyjobdam.recruit.entity.RecruitField;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;

import java.util.Set;

public record GeminiAnalysisResult(
        String companyName,
        RecruitPeriod documentPeriod,
        RecruitPeriod writtenExamPeriod,
        RecruitPeriod practicalExamPeriod,
        RecruitPeriod codingTestPeriod,
        RecruitPeriod interviewPeriod,
        /** 공고가 뽑는 직무 분야. 판단하지 못했으면 비어 있다. */
        Set<RecruitField> fields,
        String summary
) {
}
