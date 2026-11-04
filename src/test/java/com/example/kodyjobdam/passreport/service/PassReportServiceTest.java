package com.example.kodyjobdam.passreport.service;

import com.example.kodyjobdam.common.exception.PassReportException;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.passreport.dto.request.PassReportCreateDTO;
import com.example.kodyjobdam.passreport.dto.response.PassReportResponseDTO;
import com.example.kodyjobdam.passreport.entity.PassReportEntity;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
    private final User teacher = user(1L, "임경원", "", UserRole.TEACHER);
    private final User otherTeacher = user(5L, "김진로", "", UserRole.TEACHER);

    @Test
    void 면접_합격은_로그인한_학생의_학번_이름과_기업_이름_제출_시각을_저장한다() {
        // 2026-11-04 13:30 UTC = 한국 시간 22:30
        ReflectionTestUtils.setField(passReportService, "clock",
                Clock.fixed(Instant.parse("2026-11-04T13:30:00Z"), ZoneId.of("Asia/Seoul")));
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(userRepository.findByRole(UserRole.TEACHER)).thenReturn(List.of(teacher));
        when(notificationExpirationService.defaultExpiresAt()).thenReturn(EXPIRES_AT);
        givenSaved(50L);

        PassReportResponseDTO response = passReportService.create(dto(" 카카오 "), 3L);

        PassReportEntity saved = savedReport();
        assertThat(saved.getStudent()).isEqualTo(student);
        assertThat(saved.getStudentNumber()).isEqualTo("3101");
        assertThat(saved.getStudentName()).isEqualTo("홍길동");
        assertThat(saved.getCompanyName()).isEqualTo("카카오");
        assertThat(saved.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 4, 22, 30));
        assertThat(response.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 4, 22, 30));
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStudentNumber()).isEqualTo("3101");
        assertThat(response.getStudentName()).isEqualTo("홍길동");
        assertThat(response.getCompanyName()).isEqualTo("카카오");
    }

    @Test
    void 면접_합격을_보고하면_모든_진로_선생님에게_알린다() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(userRepository.findByRole(UserRole.TEACHER)).thenReturn(List.of(teacher, otherTeacher));
        when(notificationExpirationService.defaultExpiresAt()).thenReturn(EXPIRES_AT);
        givenSaved(51L);

        passReportService.create(dto("카카오"), 3L);

        for (User receiver : List.of(teacher, otherTeacher)) {
            verify(notificationService).notifyUser(
                    receiver,
                    NotificationType.PASS_REPORTED,
                    "면접 합격 보고",
                    "3101 홍길동 학생이 카카오 면접에 합격했습니다.",
                    51L,
                    "/teacher/pass-report/51",
                    EXPIRES_AT
            );
        }
    }

    @Test
    void 학번이_없는_계정은_보고할_수_없다() {
        User noNumber = user(4L, "홍길동", " ", UserRole.STUDENT);
        when(userRepository.findById(4L)).thenReturn(Optional.of(noNumber));

        assertThatThrownBy(() -> passReportService.create(dto("카카오"), 4L))
                .isInstanceOf(PassReportException.class)
                .hasMessage("학번이나 이름이 없어 합격 보고를 보낼 수 없습니다.");
        verify(passReportRepository, never()).save(any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 선생님은_전체_보고를_학번순으로_본다() {
        PassReportEntity first = report(10L, "2101", "김철수", "네이버");
        PassReportEntity second = report(11L, "3101", "홍길동", "카카오");
        when(passReportRepository.findAllByOrderByStudentNumberAscSubmittedAtAscIdAsc())
                .thenReturn(List.of(first, second));

        List<PassReportResponseDTO> responses = passReportService.readAll();

        assertThat(responses).extracting(PassReportResponseDTO::getStudentNumber).containsExactly("2101", "3101");
        assertThat(responses).extracting(PassReportResponseDTO::getCompanyName).containsExactly("네이버", "카카오");
    }

    @Test
    void 없는_보고는_찾을_수_없다() {
        when(passReportRepository.findById(50L)).thenReturn(Optional.of(report(50L, "3101", "홍길동", "카카오")));
        when(passReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(passReportService.readOne(50L).getCompanyName()).isEqualTo("카카오");
        assertThatThrownBy(() -> passReportService.readOne(99L))
                .isInstanceOf(PassReportException.class)
                .hasMessage("합격 보고를 찾을 수 없습니다.");
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

    private PassReportEntity report(Long id, String studentNumber, String studentName, String companyName) {
        return PassReportEntity.builder()
                .id(id).student(student).studentNumber(studentNumber).studentName(studentName)
                .companyName(companyName).build();
    }

    private PassReportCreateDTO dto(String companyName) {
        PassReportCreateDTO dto = new PassReportCreateDTO();
        ReflectionTestUtils.setField(dto, "companyName", companyName);
        return dto;
    }

    private User user(Long id, String name, String number, UserRole role) {
        return User.builder().id(id).name(name).student_number(number).email("u" + id + "@test.com")
                .role(role).emailVerified(true).build();
    }
}
