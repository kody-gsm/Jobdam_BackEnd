package com.example.kodyjobdam.passreport.dto.response;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import com.example.kodyjobdam.passreport.entity.PassType;
import com.example.kodyjobdam.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PassReportResponseDTO {

    private Long id;

    private PassType type;

    /** "서류" 또는 "면접" */
    private String typeLabel;

    private Long studentId;

    private String studentName;

    private String studentNumber;

    private Long teacherId;

    private String teacherName;

    private String companyName;

    private String companyPhone;

    /** 서류 합격일 때만 있다 */
    private LocalDate interviewDate;

    private LocalDateTime createdAt;

    public static PassReportResponseDTO from(PassReportEntity entity) {
        User student = entity.getStudent();
        User teacher = entity.getTeacher();

        return PassReportResponseDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .typeLabel(entity.getType().getLabel())
                .studentId(student.getId())
                .studentName(student.getName())
                .studentNumber(student.getStudent_number())
                .teacherId(teacher.getId())
                .teacherName(teacher.getName())
                .companyName(entity.getCompanyName())
                .companyPhone(entity.getCompanyPhone())
                .interviewDate(entity.getInterviewDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
