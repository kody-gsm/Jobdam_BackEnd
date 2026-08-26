package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FormSubmissionResponseDTO {

    private Long id;

    private Long formId;

    private String formTitle;

    private Long userId;

    private String userName;

    private String studentNumber;

    private LocalDateTime submittedAt;

    private List<FormAnswerResponseDTO> answers;

    public static FormSubmissionResponseDTO from(FormSubmissionEntity entity) {
        User user = entity.getUser();

        return FormSubmissionResponseDTO.builder()
                .id(entity.getId())
                .formId(entity.getForm().getId())
                .formTitle(entity.getForm().getTitle())
                .userId(user.getId())
                .userName(user.getName())
                .studentNumber(user.getStudent_number())
                .submittedAt(entity.getSubmittedAt())
                .answers(entity.getAnswers().stream()
                        .map(FormAnswerResponseDTO::from)
                        .toList())
                .build();
    }
}
