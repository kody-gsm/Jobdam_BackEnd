package com.example.kodyjobdam.passreport.dto.response;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 면접 합격 보고 한 건 */
@Getter
@Builder
public class PassReportResponseDTO {

    private Long id;

    private String studentNumber;

    private String studentName;

    private String companyName;

    /** 제출한 한국 시각 */
    private LocalDateTime submittedAt;

    public static PassReportResponseDTO from(PassReportEntity entity) {
        return PassReportResponseDTO.builder()
                .id(entity.getId())
                .studentNumber(entity.getStudentNumber())
                .studentName(entity.getStudentName())
                .companyName(entity.getCompanyName())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}
