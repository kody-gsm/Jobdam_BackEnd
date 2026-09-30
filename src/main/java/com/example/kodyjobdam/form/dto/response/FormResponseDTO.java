package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.common.dto.response.PublicationStatus;
import com.example.kodyjobdam.form.entity.FormEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FormResponseDTO {

    private Long id;

    private String title;

    private String description;

    private LocalDateTime deadline;

    /** 공개 상태. 마감일이 지났으면 저장된 상태와 무관하게 CLOSED로 나간다. */
    private PublicationStatus status;

    private List<FormQuestionResponseDTO> questions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** @param now 마감 여부를 판단할 기준 시각 (한국 시간) */
    public static FormResponseDTO from(FormEntity entity, LocalDateTime now) {
        return FormResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deadline(entity.getDeadline())
                .status(entity.publicationStatus(now))
                .questions(entity.getQuestions().stream()
                        .map(FormQuestionResponseDTO::from)
                        .toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
