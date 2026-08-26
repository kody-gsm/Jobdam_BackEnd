package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FormQuestionResponseDTO {

    private Long id;

    private int orderIndex;

    private QuestionType type;

    private String title;

    private String description;

    private boolean required;

    private List<FormQuestionOptionResponseDTO> options;

    public static FormQuestionResponseDTO from(FormQuestionEntity entity) {
        return FormQuestionResponseDTO.builder()
                .id(entity.getId())
                .orderIndex(entity.getOrderIndex())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .required(entity.isRequired())
                .options(entity.getOptions().stream()
                        .map(FormQuestionOptionResponseDTO::from)
                        .toList())
                .build();
    }
}
