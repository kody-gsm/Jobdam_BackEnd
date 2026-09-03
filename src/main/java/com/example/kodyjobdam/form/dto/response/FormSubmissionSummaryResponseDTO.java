package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 제출 목록 조회용 (답변 내용은 포함하지 않는다) */
@Getter
@Builder
public class FormSubmissionSummaryResponseDTO {

    private Long id;

    private Long userId;

    private String userName;

    private String studentNumber;

    private LocalDateTime submittedAt;

    public static FormSubmissionSummaryResponseDTO from(FormSubmissionEntity entity) {
        User user = entity.getUser();

        return FormSubmissionSummaryResponseDTO.builder()
                .id(entity.getId())
                .userId(user.getId())
                .userName(user.getName())
                .studentNumber(user.getStudent_number())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}
