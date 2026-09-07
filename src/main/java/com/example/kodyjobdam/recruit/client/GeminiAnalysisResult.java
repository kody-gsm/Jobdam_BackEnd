package com.example.kodyjobdam.recruit.client;

import com.example.kodyjobdam.recruit.entity.RecruitPeriod;

public record GeminiAnalysisResult(
        String companyName,
        RecruitPeriod documentPeriod,
        RecruitPeriod writtenExamPeriod,
        RecruitPeriod practicalExamPeriod,
        RecruitPeriod codingTestPeriod,
        RecruitPeriod interviewPeriod,
        String summary
) {
}
