package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormAnswerEntity;
import com.example.kodyjobdam.form.entity.FormAnswerOptionEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
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

    /** 파일 답변 */
    private Long fileId;

    private String fileName;

    private Long fileSize;

    private String fileDownloadUrl;

    public static FormAnswerResponseDTO from(FormAnswerEntity entity) {
        FormQuestionEntity question = entity.getQuestion();
        List<FormAnswerOptionEntity> selected = entity.getSelectedOptions();
        FormFileEntity file = entity.getFile();

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
                .fileId(file == null ? null : file.getId())
                .fileName(file == null ? null : file.getOriginalName())
                .fileSize(file == null ? null : file.getSize())
                .fileDownloadUrl(file == null ? null : FormFileResponseDTO.downloadUrl(file.getId()))
                .build();
    }
}
