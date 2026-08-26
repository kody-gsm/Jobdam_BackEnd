package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormQuestionOptionEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FormQuestionOptionResponseDTO {

    private Long id;

    private int orderIndex;

    private String label;

    public static FormQuestionOptionResponseDTO from(FormQuestionOptionEntity entity) {
        return FormQuestionOptionResponseDTO.builder()
                .id(entity.getId())
                .orderIndex(entity.getOrderIndex())
                .label(entity.getLabel())
                .build();
    }
}
