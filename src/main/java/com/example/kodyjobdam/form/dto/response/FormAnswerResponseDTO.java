package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormAnswerEntity;
import com.example.kodyjobdam.form.entity.FormAnswerOptionEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FormAnswerResponseDTO {

    private Long questionId;

    private String questionTitle;

    private QuestionType type;

    /** 주관식·숫자·날짜 답변 */
    private String textValue;

    /** 선택형 답변에서 고른 선택지 */
    private List<Long> selectedOptionIds;

    private List<String> selectedOptionLabels;

    public static FormAnswerResponseDTO from(FormAnswerEntity entity) {
        FormQuestionEntity question = entity.getQuestion();
        List<FormAnswerOptionEntity> selected = entity.getSelectedOptions();

        return FormAnswerResponseDTO.builder()
                .questionId(question.getId())
                .questionTitle(question.getTitle())
                .type(question.getType())
                .textValue(entity.getTextValue())
                .selectedOptionIds(selected.stream()
                        .map(selectedOption -> selectedOption.getOption().getId())
                        .toList())
                .selectedOptionLabels(selected.stream()
                        .map(selectedOption -> selectedOption.getOption().getLabel())
                        .toList())
                .build();
    }
}
