package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
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

    private FormStatus status;

    private List<FormQuestionResponseDTO> questions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static FormResponseDTO from(FormEntity entity) {
        return FormResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deadline(entity.getDeadline())
                .status(entity.getStatus())
                .questions(entity.getQuestions().stream()
                        .map(FormQuestionResponseDTO::from)
                        .toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
