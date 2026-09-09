package com.example.kodyjobdam.form.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class FormAnswerDTO {

    @NotNull(message = "질문 ID가 필요합니다.")
    private Long questionId;

    /** 주관식·숫자·날짜 답변 */
    private String textValue;

    /** 선택형 답변에서 고른 선택지 ID 목록 */
    private List<Long> optionIds;

    /** 파일 질문에 첨부할 파일 ID (업로드 API가 돌려준 값) */
    private Long fileId;
}
