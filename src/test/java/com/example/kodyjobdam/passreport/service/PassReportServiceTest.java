package com.example.kodyjobdam.passreport.service;

import com.example.kodyjobdam.common.exception.PassReportException;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.passreport.dto.request.PassReportCreateDTO;
import com.example.kodyjobdam.passreport.dto.response.PassReportResponseDTO;
import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import com.example.kodyjobdam.passreport.entity.PassType;
import com.example.kodyjobdam.passreport.repository.PassReportRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassReportServiceTest {

    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2027, 1, 18, 0, 0);

    @Mock
    private PassReportRepository passReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private PassReportService passReportService;

    private final User student = user(3L, "홍길동", "3101", UserRole.STUDENT);
    private final User teacher = user(2L, "임경원", "T002", UserRole.TEACHER);

    @Test
    void 서류_합격은_면접_날짜와_함께_저장하고_선생님에게_알린다() {
        givenUsers();
        givenSaved(50L);

        PassReportResponseDTO response = passReportService.create(
                dto(PassType.DOCUMENT, " 카카오 ", "02-123-4567", LocalDate.of(2026, 11, 2)), 3L);

        PassReportEntity saved = savedReport();
        assertThat(saved.getStudent()).isEqualTo(student);
        assertThat(saved.getTeacher()).isEqualTo(teacher);
        assertThat(saved.getCompanyName()).isEqualTo("카카오");
        assertThat(saved.getInterviewDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(response.getTypeLabel()).isEqualTo("서류");
        assertThat(response.getStudentNumber()).isEqualTo("3101");
        verify(notificationService).notifyUser(
                teacher,
                NotificationType.PASS_REPORTED,
                "서류 합격 보고",
                "3101 홍길동 학생이 카카오 서류에 합격했습니다. 면접일: 2026-11-02",
                50L,
                "/teacher/pass-report/50",
                EXPIRES_AT
        );
    }

    @Test
    void 면접_합격은_면접_날짜를_보내도_남기지_않는다() {
        givenUsers();
        givenSaved(51L);

        passReportService.create(dto(PassType.INTERVIEW, "카카오", "1588-1234", LocalDate.of(2026, 11, 2)), 3L);

        PassReportEntity saved = savedReport();
        assertThat(saved.getType()).isEqualTo(PassType.INTERVIEW);
        assertThat(saved.getInterviewDate()).isNull();
        verify(notificationService).notifyUser(
                teacher,
                NotificationType.PASS_REPORTED,
                "면접 합격 보고",
                "3101 홍길동 학생이 카카오 면접에 합격했습니다.",
                51L,
                "/teacher/pass-report/51",
                EXPIRES_AT
        );
    }

    @Test
    void 서류_합격인데_면접_날짜가_없으면_보낼_수_없다() {
        assertThatThrownBy(() -> passReportService.create(dto(PassType.DOCUMENT, "카카오", "02-123-4567", null), 3L))
                .isInstanceOf(PassReportException.class)
                .hasMessage("서류 합격은 면접 날짜를 입력해주세요.");
        verify(passReportRepository, never()).save(any());
    }

    @Test
    void 진로_선생님이_아니면_보낼_수_없다() {
        User weeTeacher = user(4L, "위클래스", "T004", UserRole.WEE_TEACHER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(userRepository.findById(4L)).thenReturn(Optional.of(weeTeacher));
        PassReportCreateDTO dto = dto(PassType.INTERVIEW, "카카오", "02-123-4567", null);
        ReflectionTestUtils.setField(dto, "teacherId", 4L);

        assertThatThrownBy(() -> passReportService.create(dto, 3L))
                .isInstanceOf(PassReportException.class)
                .hasMessage("진로 선생님에게만 보낼 수 있습니다.");
        verify(passReportRepository, never()).save(any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 다른_선생님이_받은_보고는_볼_수_없다() {
        PassReportEntity report = PassReportEntity.builder()
                .id(50L).student(student).teacher(teacher).type(PassType.INTERVIEW)
                .companyName("카카오").companyPhone("02-123-4567").build();
        when(passReportRepository.findById(50L)).thenReturn(Optional.of(report));

        assertThat(passReportService.readReceivedOne(50L, 2L).getCompanyName()).isEqualTo("카카오");
        assertThatThrownBy(() -> passReportService.readReceivedOne(50L, 99L))
                .isInstanceOf(PassReportException.class)
                .hasMessage("합격 보고를 찾을 수 없습니다.");
    }

    private void givenUsers() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(notificationExpirationService.defaultExpiresAt()).thenReturn(EXPIRES_AT);
    }

    /** 저장소가 받은 엔티티에 id를 붙여 그대로 돌려준다. */
    private void givenSaved(Long id) {
        when(passReportRepository.save(any(PassReportEntity.class))).thenAnswer(invocation -> {
            PassReportEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", id);
            return entity;
        });
    }

    private PassReportEntity savedReport() {
        ArgumentCaptor<PassReportEntity> captor = ArgumentCaptor.forClass(PassReportEntity.class);
        verify(passReportRepository).save(captor.capture());
        return captor.getValue();
    }

    private PassReportCreateDTO dto(PassType type, String companyName, String companyPhone, LocalDate interviewDate) {
        PassReportCreateDTO dto = new PassReportCreateDTO();
        ReflectionTestUtils.setField(dto, "teacherId", 2L);
        ReflectionTestUtils.setField(dto, "type", type);
        ReflectionTestUtils.setField(dto, "companyName", companyName);
        ReflectionTestUtils.setField(dto, "companyPhone", companyPhone);
        ReflectionTestUtils.setField(dto, "interviewDate", interviewDate);
        return dto;
    }

    private User user(Long id, String name, String number, UserRole role) {
        return User.builder().id(id).name(name).student_number(number).email("u" + id + "@test.com")
                .role(role).emailVerified(true).build();
    }
}
