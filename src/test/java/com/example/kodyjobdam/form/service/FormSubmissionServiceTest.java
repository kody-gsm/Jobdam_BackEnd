package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.request.FormAnswerDTO;
import com.example.kodyjobdam.form.dto.request.FormSubmitDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionResponseDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormFileRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormSubmissionServiceTest {

    @Mock
    private FormSubmissionRepository submissionRepository;

    @Mock
    private FormRepository formRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FormFileRepository fileRepository;

    @InjectMocks
    private FormSubmissionService formSubmissionService;

    @Test
    void 제출한_응답을_다시_작성하면_답변이_교체된다() {
        FormEntity form = publishedForm();
        FormSubmissionEntity submission = submissionOf(form, "이전 답변");
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L)).thenReturn(Optional.of(submission));

        FormSubmissionResponseDTO response = formSubmissionService.resubmit(1L, submitDto("새 답변"), 3L);

        assertThat(submission.getAnswers()).hasSize(1);
        assertThat(submission.getAnswers().get(0).getTextValue()).isEqualTo("새 답변");
        assertThat(response.getAnswers()).hasSize(1);
    }

    @Test
    void 제출한_응답이_없으면_수정할_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(publishedForm()));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, submitDto("새 답변"), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("아직 제출한 응답이 없습니다.");
    }

    @Test
    void 마감된_폼은_응답을_수정할_수_없다() {
        FormEntity form = publishedForm();
        form.close();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, submitDto("새 답변"), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("지금은 응답을 받지 않는 폼입니다.");
    }

    @Test
    void 필수_질문을_비우고_수정할_수_없다() {
        FormEntity form = publishedForm();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(submissionOf(form, "이전 답변")));

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, submitDto("   "), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("필수 질문에 답변해주세요: 질문");
    }

    @Test
    void 본인이_올린_파일은_답변에_첨부된다() {
        FormEntity form = formWithFileQuestion();
        FormSubmissionEntity submission = FormSubmissionEntity.builder()
                .id(100L)
                .form(form)
                .user(user(3L))
                .build();
        FormFileEntity file = fileOf(form, user(3L));
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L)).thenReturn(Optional.of(submission));
        when(fileRepository.findById(50L)).thenReturn(Optional.of(file));

        formSubmissionService.resubmit(1L, fileSubmitDto(50L), 3L);

        assertThat(submission.getAnswers()).hasSize(1);
        assertThat(submission.getAnswers().get(0).getFile()).isSameAs(file);
    }

    @Test
    void 남이_올린_파일은_첨부할_수_없다() {
        FormEntity form = formWithFileQuestion();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(FormSubmissionEntity.builder().id(100L).form(form).user(user(3L)).build()));
        when(fileRepository.findById(50L)).thenReturn(Optional.of(fileOf(form, user(4L))));

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, fileSubmitDto(50L), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("본인이 올린 파일만 첨부할 수 있습니다: 포트폴리오");
    }

    @Test
    void 다른_폼에_올린_파일은_첨부할_수_없다() {
        FormEntity form = formWithFileQuestion();
        FormEntity otherForm = FormEntity.builder().id(9L).title("다른 폼").user(user(2L)).build();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(FormSubmissionEntity.builder().id(100L).form(form).user(user(3L)).build()));
        when(fileRepository.findById(50L)).thenReturn(Optional.of(fileOf(otherForm, user(3L))));

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, fileSubmitDto(50L), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("이 폼에 올린 파일이 아닙니다: 포트폴리오");
    }

    @Test
    void 파일을_첨부하지_않으면_필수_파일_질문을_통과할_수_없다() {
        FormEntity form = formWithFileQuestion();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.findByFormIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(FormSubmissionEntity.builder().id(100L).form(form).user(user(3L)).build()));

        assertThatThrownBy(() -> formSubmissionService.resubmit(1L, fileSubmitDto(null), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("필수 질문에 답변해주세요: 포트폴리오");
    }

    private FormEntity formWithFileQuestion() {
        FormEntity form = FormEntity.builder()
                .id(1L)
                .title("지원서")
                .user(user(2L))
                .status(FormStatus.PUBLISHED)
                .build();
        form.addQuestion(FormQuestionEntity.builder()
                .id(20L)
                .orderIndex(1)
                .type(QuestionType.FILE)
                .title("포트폴리오")
                .required(true)
                .build());
        return form;
    }

    private FormFileEntity fileOf(FormEntity form, User owner) {
        return FormFileEntity.builder()
                .id(50L)
                .form(form)
                .user(owner)
                .originalName("portfolio.pdf")
                .storedName("2026/09/uuid.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .build();
    }

    private FormSubmitDTO fileSubmitDto(Long fileId) {
        FormAnswerDTO answer = new FormAnswerDTO();
        ReflectionTestUtils.setField(answer, "questionId", 20L);
        ReflectionTestUtils.setField(answer, "fileId", fileId);

        FormSubmitDTO dto = new FormSubmitDTO();
        ReflectionTestUtils.setField(dto, "answers", List.of(answer));
        return dto;
    }

    private FormEntity publishedForm() {
        FormEntity form = FormEntity.builder()
                .id(1L)
                .title("폼")
                .user(user(2L))
                .status(FormStatus.PUBLISHED)
                .build();
        form.addQuestion(FormQuestionEntity.builder()
                .id(10L)
                .orderIndex(1)
                .type(QuestionType.SHORT_TEXT)
                .title("질문")
                .required(true)
                .build());
        return form;
    }

    private FormSubmissionEntity submissionOf(FormEntity form, String textValue) {
        FormSubmissionEntity submission = FormSubmissionEntity.builder()
                .id(100L)
                .form(form)
                .user(user(3L))
                .build();
        submission.addAnswer(com.example.kodyjobdam.form.entity.FormAnswerEntity.builder()
                .question(form.getQuestions().get(0))
                .textValue(textValue)
                .build());
        return submission;
    }

    private FormSubmitDTO submitDto(String textValue) {
        FormAnswerDTO answer = new FormAnswerDTO();
        ReflectionTestUtils.setField(answer, "questionId", 10L);
        ReflectionTestUtils.setField(answer, "textValue", textValue);

        FormSubmitDTO dto = new FormSubmitDTO();
        ReflectionTestUtils.setField(dto, "answers", List.of(answer));
        return dto;
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("사용자" + id)
                .role(UserRole.STUDENT)
                .build();
    }
}
