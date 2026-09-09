package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.request.FormCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormUpdateDTO;
import com.example.kodyjobdam.form.dto.request.FormQuestionCreateDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRepository formRepository;

    @Mock
    private FormSubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private FormService formService;

    @Test
    void 응답이_없으면_폼을_삭제한다() {
        FormEntity form = draftForm();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.existsByFormId(1L)).thenReturn(false);

        formService.delete(1L, 2L);

        verify(formRepository).delete(form);
    }

    @Test
    void 응답이_있으면_폼을_삭제할_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(draftForm()));
        when(submissionRepository.existsByFormId(1L)).thenReturn(true);

        assertThatThrownBy(() -> formService.delete(1L, 2L))
                .isInstanceOf(FormException.class)
                .hasMessage("응답이 제출된 폼은 삭제할 수 없습니다.");
        verify(formRepository, never()).delete(any(FormEntity.class));
    }

    @Test
    void 다른_선생님은_폼을_삭제할_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(draftForm()));

        assertThatThrownBy(() -> formService.delete(1L, 99L))
                .isInstanceOf(FormException.class);
        verify(formRepository, never()).delete(any(FormEntity.class));
    }

    @Test
    void 공개된_폼도_수정할_수_있다() {
        FormEntity form = FormEntity.builder()
                .id(1L)
                .title("이전 제목")
                .user(user(2L))
                .status(FormStatus.PUBLISHED)
                .build();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        formService.update(1L, updateDtoWithoutQuestions("새 제목"), 2L);

        assertThat(form.getTitle()).isEqualTo("새 제목");
    }

    @Test
    void 응답이_있으면_질문을_바꿀_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(draftForm()));
        when(submissionRepository.existsByFormId(1L)).thenReturn(true);

        assertThatThrownBy(() -> formService.update(1L, updateDtoWithQuestions(), 2L))
                .isInstanceOf(FormException.class)
                .hasMessage("이미 응답이 제출된 폼은 질문을 바꿀 수 없습니다.");
    }

    private FormEntity draftForm() {
        return FormEntity.builder()
                .id(1L)
                .title("폼")
                .user(user(2L))
                .status(FormStatus.DRAFT)
                .build();
    }

    @Test
    void createDraftDoesNotCreateNotification() {
        User teacher = user(2L);
        FormCreateDTO dto = createDto();
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(formRepository.save(any(FormEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        formService.create(dto, 2L);

        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishCreatesStudentNotifications() {
        FormEntity form = FormEntity.builder()
                .id(10L)
                .user(user(2L))
                .title("만족도 조사")
                .description("내용")
                .status(FormStatus.DRAFT)
                .build();
        form.addQuestion(FormQuestionEntity.builder()
                .orderIndex(1)
                .type(QuestionType.SHORT_TEXT)
                .title("질문")
                .build());
        when(formRepository.findById(10L)).thenReturn(Optional.of(form));
        when(notificationExpirationService.formExpiresAt(form.getDeadline()))
                .thenReturn(LocalDateTime.of(2026, 10, 10, 0, 0));

        formService.publish(10L, 2L);

        verify(notificationService).notifyAllStudents(
                eq(NotificationType.FORM_PUBLISHED),
                eq("새로운 폼"),
                eq("만족도 조사 폼이 게시되었습니다."),
                eq(10L),
                eq("/form/10"),
                eq(LocalDateTime.of(2026, 10, 10, 0, 0))
        );
    }

    @Test
    void publishAlreadyPublishedFormDoesNotCreateDuplicateNotification() {
        FormEntity form = FormEntity.builder()
                .id(10L)
                .user(user(2L))
                .title("만족도 조사")
                .status(FormStatus.PUBLISHED)
                .build();
        when(formRepository.findById(10L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.publish(10L, 2L))
                .isInstanceOf(FormException.class);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishOtherTeachersFormThrowsForbidden() {
        FormEntity form = FormEntity.builder()
                .id(10L)
                .user(user(2L))
                .title("만족도 조사")
                .status(FormStatus.DRAFT)
                .build();
        when(formRepository.findById(10L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.publish(10L, 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("폼을 관리할 권한이 없습니다.");
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 채용_공고용_기본_폼은_학번_이름_포트폴리오_질문으로_만들어진다() {
        when(formRepository.save(any(FormEntity.class))).thenAnswer(returnsFirstArg());

        FormEntity form = formService.createForRecruit(
                user(2L), "잡담", LocalDateTime.of(2026, 9, 10, 23, 59, 59));

        assertThat(form.getTitle()).isEqualTo("잡담 지원서");
        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(form.getDeadline()).isEqualTo(LocalDateTime.of(2026, 9, 10, 23, 59, 59));
        assertThat(form.getQuestions())
                .extracting(FormQuestionEntity::getTitle)
                .containsExactly("학번", "이름", "포트폴리오");
        assertThat(form.getQuestions())
                .allSatisfy(question -> assertThat(question.getType()).isEqualTo(QuestionType.SHORT_TEXT));
    }

    @Test
    void 공고_공개시_초안인_지원_폼은_알림_없이_공개된다() {
        FormEntity form = draftForm();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        formService.publishForRecruit(1L);

        assertThat(form.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 공고_삭제시_응답이_있는_지원_폼은_남긴다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(draftForm()));
        when(submissionRepository.existsByFormId(1L)).thenReturn(true);

        formService.deleteForRecruit(1L);

        verify(formRepository, never()).delete(any(FormEntity.class));
    }

    @Test
    void 공고_삭제시_응답이_없는_지원_폼은_함께_지운다() {
        FormEntity form = draftForm();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(submissionRepository.existsByFormId(1L)).thenReturn(false);

        formService.deleteForRecruit(1L);

        verify(formRepository).delete(form);
    }

    private FormUpdateDTO updateDtoWithoutQuestions(String title) {
        FormUpdateDTO dto = new FormUpdateDTO();
        ReflectionTestUtils.setField(dto, "title", title);
        return dto;
    }

    private FormUpdateDTO updateDtoWithQuestions() {
        FormQuestionCreateDTO question = new FormQuestionCreateDTO();
        ReflectionTestUtils.setField(question, "type", QuestionType.SHORT_TEXT);
        ReflectionTestUtils.setField(question, "title", "바뀐 질문");
        ReflectionTestUtils.setField(question, "required", true);

        FormUpdateDTO dto = new FormUpdateDTO();
        ReflectionTestUtils.setField(dto, "title", "제목");
        ReflectionTestUtils.setField(dto, "questions", List.of(question));
        return dto;
    }

    private FormCreateDTO createDto() {
        FormQuestionCreateDTO question = new FormQuestionCreateDTO();
        ReflectionTestUtils.setField(question, "type", QuestionType.SHORT_TEXT);
        ReflectionTestUtils.setField(question, "title", "질문");
        ReflectionTestUtils.setField(question, "required", true);

        FormCreateDTO dto = new FormCreateDTO();
        ReflectionTestUtils.setField(dto, "title", "만족도 조사");
        ReflectionTestUtils.setField(dto, "description", "내용");
        ReflectionTestUtils.setField(dto, "questions", List.of(question));
        return dto;
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("선생님")
                .student_number("T" + id)
                .email("teacher" + id + "@test.com")
                .role(UserRole.TEACHER)
                .emailVerified(true)
                .build();
    }
}
