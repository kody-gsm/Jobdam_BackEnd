package com.example.kodyjobdam.recruit.dto.response;

import com.example.kodyjobdam.recruit.dto.RecruitPeriodDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecruitResponseDTO {

    private Long id;

    private String companyName;

    private RecruitPeriodDTO documentPeriod;

    private RecruitPeriodDTO writtenExamPeriod;

    private RecruitPeriodDTO practicalExamPeriod;

    private RecruitPeriodDTO codingTestPeriod;

    private RecruitPeriodDTO interviewPeriod;

    private String summary;

    private RecruitStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static RecruitResponseDTO from(RecruitEntity entity) {
        return RecruitResponseDTO.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .documentPeriod(RecruitPeriodDTO.from(entity.getDocumentPeriod()))
                .writtenExamPeriod(RecruitPeriodDTO.from(entity.getWrittenExamPeriod()))
                .practicalExamPeriod(RecruitPeriodDTO.from(entity.getPracticalExamPeriod()))
                .codingTestPeriod(RecruitPeriodDTO.from(entity.getCodingTestPeriod()))
                .interviewPeriod(RecruitPeriodDTO.from(entity.getInterviewPeriod()))
                .summary(entity.getSummary())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
