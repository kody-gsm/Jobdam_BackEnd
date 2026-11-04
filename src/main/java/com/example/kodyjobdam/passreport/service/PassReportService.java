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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** 학생이 면접 합격을 진로 선생님들에게 알린다. */
@Service
@RequiredArgsConstructor
public class PassReportService {

    private static final String NOTIFICATION_TITLE = "면접 합격 보고";

    private final PassReportRepository passReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationExpirationService notificationExpirationService;

    // 테스트에서 고정 시각을 넣을 수 있게 final로 두지 않는다.
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    /** 학생: 면접 합격 보고를 남기고 모든 진로 선생님에게 알린다. */
    @Transactional
    public PassReportResponseDTO create(PassReportCreateDTO dto, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> PassReportException.notFound("회원이 없습니다."));
        if (!StringUtils.hasText(student.getStudent_number()) || !StringUtils.hasText(student.getName())) {
            throw PassReportException.badRequest("학번이나 이름이 없어 합격 보고를 보낼 수 없습니다.");
        }

        PassReportEntity report = passReportRepository.save(PassReportEntity.builder()
                .student(student)
                .studentNumber(student.getStudent_number().trim())
                .studentName(student.getName().trim())
                .companyName(dto.getCompanyName().trim())
                .submittedAt(LocalDateTime.now(clock))
                .build());

        notifyCareerTeachers(report);
        return PassReportResponseDTO.from(report);
    }

    /** 학생: 내가 보낸 면접 합격 보고 */
    @Transactional(readOnly = true)
    public List<PassReportResponseDTO> readMine(Long studentId) {
        return passReportRepository.findByStudent_IdOrderBySubmittedAtDescIdDesc(studentId).stream()
                .map(PassReportResponseDTO::from)
                .toList();
    }

    /** 선생님: 지금까지 들어온 면접 합격 보고 전체를 학번순으로 */
    @Transactional(readOnly = true)
    public List<PassReportResponseDTO> readAll() {
        return passReportRepository.findAllByOrderByStudentNumberAscSubmittedAtAscIdAsc().stream()
                .map(PassReportResponseDTO::from)
                .toList();
    }

    /** 선생님: 면접 합격 보고 한 건. 알림을 누르면 여기로 온다. */
    @Transactional(readOnly = true)
    public PassReportResponseDTO readOne(Long reportId) {
        return passReportRepository.findById(reportId)
                .map(PassReportResponseDTO::from)
                .orElseThrow(() -> PassReportException.notFound("합격 보고를 찾을 수 없습니다."));
    }

    private void notifyCareerTeachers(PassReportEntity report) {
        String content = notificationContent(report);
        String targetUrl = "/teacher/pass-report/" + report.getId();
        LocalDateTime expiresAt = notificationExpirationService.defaultExpiresAt();

        for (User teacher : userRepository.findByRole(UserRole.TEACHER)) {
            notificationService.notifyUser(
                    teacher,
                    NotificationType.PASS_REPORTED,
                    NOTIFICATION_TITLE,
                    content,
                    report.getId(),
                    targetUrl,
                    expiresAt
            );
        }
    }

    /** 예) "3101 홍길동 학생이 카카오 면접에 합격했습니다." */
    private String notificationContent(PassReportEntity report) {
        return report.getStudentNumber() + " " + report.getStudentName() + " 학생이 "
                + report.getCompanyName() + " 면접에 합격했습니다.";
    }
}
