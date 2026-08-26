package com.example.kodyjobdam.form.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class FormSubmitDTO {

    @Valid
    @NotEmpty(message = "답변을 1개 이상 보내주세요.")
    private List<FormAnswerDTO> answers;
}
