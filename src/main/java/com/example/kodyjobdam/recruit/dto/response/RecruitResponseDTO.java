package com.example.kodyjobdam.recruit.dto.response;

import com.example.kodyjobdam.recruit.entity.RecruitEntity;
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

    private LocalDateTime createdAt;

    public static RecruitResponseDTO from(RecruitEntity entity) {
        return RecruitResponseDTO.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .interviewDate(entity.getInterviewDate())
                .deadline(entity.getDeadline())
                .summary(entity.getSummary())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
