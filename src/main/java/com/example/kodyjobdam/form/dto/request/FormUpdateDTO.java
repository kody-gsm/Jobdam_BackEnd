package com.example.kodyjobdam.form.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class FormUpdateDTO {

    @NotBlank(message = "폼 제목을 입력해주세요.")
    private String title;

    /** 생략하면 기존 설명을 그대로 둔다 */
    private String description;

    /**
     * 생략하면 기존 마감일을 그대로 둔다.
     * 공고에 딸린 지원 폼은 공고의 서류 접수 종료일을 따르므로 여기서 바꿀 수 없다.
     */
    private LocalDateTime deadline;

    /** 보내면 질문 전체가 교체되고, 생략하면 기존 질문을 그대로 둔다 */
    @Valid
    private List<FormQuestionCreateDTO> questions;
}
