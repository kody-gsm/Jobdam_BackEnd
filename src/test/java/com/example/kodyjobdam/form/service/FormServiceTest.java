package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.request.FormCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormQuestionCreateDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRepository formRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private FormService formService;

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
