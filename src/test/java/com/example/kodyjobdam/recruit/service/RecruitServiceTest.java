package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.recruit.client.GeminiAnalysisResult;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class RecruitServiceTest {

    @Mock
    private RecruitRepository recruitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private FormService formService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private RecruitService recruitService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzeCreatesDefaultApplicationForm() {
        User teacher = user(2L);
        RecruitPeriod documentPeriod = new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
        FormEntity form = FormEntity.builder().id(7L).title("잡담 지원서").status(FormStatus.DRAFT).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(geminiClient.analyze(any(), eq("image/png"))).thenReturn(new GeminiAnalysisResult(
                "잡담", documentPeriod, null, null, null, null, "요약"));
        when(formService.createForRecruit(
                eq(teacher), eq("잡담"), eq(LocalDate.of(2026, 9, 10).atTime(LocalTime.MAX))))
                .thenReturn(form);
        when(recruitRepository.save(any(RecruitEntity.class))).thenAnswer(returnsFirstArg());

        RecruitResponseDTO response = recruitService.analyze(
                new MockMultipartFile("image", "recruit.png", "image/png", new byte[]{1, 2}), 2L);

        assertThat(response.getFormId()).isEqualTo(7L);
    }

    @Test
    void publishAlsoPublishesApplicationForm() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.DRAFT).build())
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        recruitService.publish(10L, 2L);

        verify(formService).publishForRecruit(7L);
    }

    @Test
    void deleteAlsoRemovesApplicationForm() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .form(FormEntity.builder().id(7L).status(FormStatus.DRAFT).build())
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        recruitService.delete(10L, 2L);

        verify(recruitRepository).delete(recruit);
        verify(formService).deleteForRecruit(7L);
    }

    @Test
    void publishCreatesStudentNotifications() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));
        when(notificationExpirationService.recruitExpiresAt("2026-09-10"))
                .thenReturn(LocalDateTime.of(2026, 10, 10, 23, 59, 59));

        recruitService.publish(10L, 2L);

        verify(notificationService).notifyAllStudents(
                eq(NotificationType.RECRUIT_PUBLISHED),
                eq("새로운 취업 공지"),
                eq("잡담 취업 공지가 게시되었습니다."),
                eq(10L),
                eq("/recruit/10"),
                eq(LocalDateTime.of(2026, 10, 10, 23, 59, 59))
        );
    }

    @Test
    void publishAlreadyPublishedRecruitDoesNotCreateDuplicateNotification() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.PUBLISHED)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L, 2L))
                .isInstanceOf(RecruitException.class);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishOtherTeachersRecruitThrowsForbidden() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L, 3L))
                .isInstanceOf(RecruitException.class)
                .hasMessage("채용 공고를 관리할 권한이 없습니다.");
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateWithDeadlineAndInterviewDateKeepsOtherPeriods() throws Exception {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .documentPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))
                .codingTestPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15)))
                .interviewPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)))
                .summary("요약")
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue(
                "{\"companyName\":\"잡담\",\"deadline\":\"2026-09-12\","
                        + "\"interviewDate\":\"2026-09-21 ~ 2026-09-22\",\"summary\":\"요약\"}",
                RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getDeadline()).isEqualTo("2026-09-12");
        assertThat(response.getInterviewDate()).isEqualTo("2026-09-21 ~ 2026-09-22");
        assertThat(response.getDocumentPeriod().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getDocumentPeriod().endDate()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(response.getCodingTestPeriod().startDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void updateWithBlankInterviewDateClearsInterviewPeriod() throws Exception {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .interviewPeriod(new RecruitPeriod(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)))
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        RecruitUpdateDTO dto = objectMapper.readValue("{\"interviewDate\":\"\"}", RecruitUpdateDTO.class);

        RecruitResponseDTO response = recruitService.update(10L, dto, 2L);

        assertThat(response.getInterviewDate()).isNull();
        assertThat(response.getInterviewPeriod()).isNull();
        assertThat(response.getCompanyName()).isEqualTo("잡담");
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
