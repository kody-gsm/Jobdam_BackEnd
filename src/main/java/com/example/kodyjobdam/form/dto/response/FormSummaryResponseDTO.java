package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 목록 조회용 (질문은 포함하지 않는다) */
@Getter
@Builder
public class FormSummaryResponseDTO {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime deadline;

    private FormStatus status;

    private int questionCount;

    private LocalDateTime createdAt;

    public static FormSummaryResponseDTO from(FormEntity entity) {
        return FormSummaryResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deadline(entity.getDeadline())
                .status(entity.getStatus())
                .questionCount(entity.getQuestions().size())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
