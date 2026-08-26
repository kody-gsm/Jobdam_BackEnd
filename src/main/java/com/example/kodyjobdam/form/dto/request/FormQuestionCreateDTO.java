package com.example.kodyjobdam.form.dto.request;

import com.example.kodyjobdam.form.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class FormQuestionCreateDTO {

    @NotNull(message = "질문 유형을 선택해주세요.")
    private QuestionType type;

    @NotBlank(message = "질문 제목을 입력해주세요.")
    private String title;

    private String description;

    private boolean required;

    /** 선택형 질문의 보기 목록 (주관식에서는 비워둔다) */
    private List<String> options;
}
