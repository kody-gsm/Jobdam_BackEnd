package com.example.kodyjobdam.recruit.dto.response;

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

    private String interviewDate;

    private String deadline;

    private String summary;

    private RecruitStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static RecruitResponseDTO from(RecruitEntity entity) {
        return RecruitResponseDTO.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .interviewDate(entity.getInterviewDate())
                .deadline(entity.getDeadline())
                .summary(entity.getSummary())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
