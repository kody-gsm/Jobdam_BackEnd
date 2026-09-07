package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.response.FormSubmissionResponseDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.entity.SubmissionStatus;
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

    @InjectMocks
    private FormSubmissionService formSubmissionService;

    @Test
    void 지원자를_확정한다() {
        FormSubmissionEntity submission = submission(SubmissionStatus.SUBMITTED);
        givenSubmission(submission);

        FormSubmissionResponseDTO result = formSubmissionService.confirm(1L, 10L, 2L);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.CONFIRMED);
    }

    @Test
    void 이미_확정된_지원자는_다시_확정할_수_없다() {
        givenSubmission(submission(SubmissionStatus.CONFIRMED));

        assertThatThrownBy(() -> formSubmissionService.confirm(1L, 10L, 2L))
                .isInstanceOf(FormException.class)
                .hasMessage("이미 확정된 지원자입니다.");
    }

    @Test
    void 확정을_되돌린다() {
        FormSubmissionEntity submission = submission(SubmissionStatus.SUBMITTED);
        submission.confirm();
        givenSubmission(submission);

        FormSubmissionResponseDTO result = formSubmissionService.cancelConfirm(1L, 10L, 2L);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(result.getConfirmedAt()).isNull();
        assertThat(submission.getConfirmedAt()).isNull();
    }

    @Test
    void 확정하지_않은_지원자는_되돌릴_수_없다() {
        givenSubmission(submission(SubmissionStatus.SUBMITTED));

        assertThatThrownBy(() -> formSubmissionService.cancelConfirm(1L, 10L, 2L))
                .isInstanceOf(FormException.class)
                .hasMessage("확정되지 않은 지원자입니다.");
    }

    @Test
    void 다른_선생님은_지원자를_확정할_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(form()));

        assertThatThrownBy(() -> formSubmissionService.confirm(1L, 10L, 99L))
                .isInstanceOf(FormException.class)
                .hasMessage("폼 제출 내역에 접근할 권한이 없습니다.");
    }

    private void givenSubmission(FormSubmissionEntity submission) {
        when(formRepository.findById(1L)).thenReturn(Optional.of(submission.getForm()));
        when(submissionRepository.findById(10L)).thenReturn(Optional.of(submission));
    }

    private FormEntity form() {
        return FormEntity.builder()
                .id(1L)
                .title("취업 지원서")
                .user(user(2L, UserRole.TEACHER))
                .build();
    }

    private FormSubmissionEntity submission(SubmissionStatus status) {
        return FormSubmissionEntity.builder()
                .id(10L)
                .form(form())
                .user(user(3L, UserRole.STUDENT))
                .status(status)
                .build();
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .name("사용자" + id)
                .student_number("300" + id)
                .email("user" + id + "@test.com")
                .role(role)
                .emailVerified(true)
                .build();
    }
}
