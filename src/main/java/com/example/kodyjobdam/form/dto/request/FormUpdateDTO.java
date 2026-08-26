package com.example.kodyjobdam.form.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class FormUpdateDTO {

    @NotBlank(message = "폼 제목을 입력해주세요.")
    private String title;

    private String description;

    /** 질문은 부분 수정이 아니라 통째로 교체된다 */
    @Valid
    @NotEmpty(message = "질문을 1개 이상 추가해주세요.")
    private List<FormQuestionCreateDTO> questions;
}
