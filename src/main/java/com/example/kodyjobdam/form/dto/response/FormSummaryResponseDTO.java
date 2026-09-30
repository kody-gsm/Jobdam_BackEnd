package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.common.dto.response.PublicationStatus;
import com.example.kodyjobdam.form.entity.FormEntity;
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

    /** 공개 상태. 마감일이 지났으면 저장된 상태와 무관하게 CLOSED로 나간다. */
    private PublicationStatus status;

    private int questionCount;

    private LocalDateTime createdAt;

    /** @param now 마감 여부를 판단할 기준 시각 (한국 시간) */
    public static FormSummaryResponseDTO from(FormEntity entity, LocalDateTime now) {
        return FormSummaryResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deadline(entity.getDeadline())
                .status(entity.publicationStatus(now))
                .questionCount(entity.getQuestions().size())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
