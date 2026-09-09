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

    /** 지원 마감일(서류 접수 종료일). documentPeriod에서 뽑아낸 표기용 값이다. */
    private String deadline;

    /** 면접 일정 표기용 값. interviewPeriod에서 뽑아낸다. */
    private String interviewDate;

    private String summary;

    /** 공고와 함께 만들어진 지원 폼 식별자 */
    private Long formId;

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
                .deadline(entity.getDeadline())
                .interviewDate(entity.getInterviewDate())
                .summary(entity.getSummary())
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
