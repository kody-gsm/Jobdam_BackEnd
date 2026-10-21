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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 학생이 서류·면접 합격을 진로 선생님에게 알린다. */
@Service
@RequiredArgsConstructor
public class PassReportService {

    private final PassReportRepository passReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationExpirationService notificationExpirationService;

    /** 학생: 합격 보고를 보내고 받는 선생님에게 알린다. */
    @Transactional
    public PassReportResponseDTO create(PassReportCreateDTO dto, Long studentId) {
        PassType type = dto.getType();
        if (type.requiresInterviewDate() && dto.getInterviewDate() == null) {
            throw PassReportException.badRequest("서류 합격은 면접 날짜를 입력해주세요.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> PassReportException.notFound("회원이 없습니다."));
        User teacher = findCareerTeacher(dto.getTeacherId());

        PassReportEntity report = passReportRepository.save(PassReportEntity.builder()
                .student(student)
                .teacher(teacher)
                .type(type)
                .companyName(dto.getCompanyName().trim())
                .companyPhone(dto.getCompanyPhone().trim())
                // 면접 합격 보고에는 면접 날짜가 없다. 보내 와도 남기지 않는다.
                .interviewDate(type.requiresInterviewDate() ? dto.getInterviewDate() : null)
                .build());

        notificationService.notifyUser(
                teacher,
                NotificationType.PASS_REPORTED,
                type.getLabel() + " 합격 보고",
                notificationContent(report),
                report.getId(),
                "/teacher/pass-report/" + report.getId(),
                notificationExpirationService.defaultExpiresAt()
        );
        return PassReportResponseDTO.from(report);
    }

    /** 학생: 내가 보낸 합격 보고 */
    @Transactional(readOnly = true)
    public List<PassReportResponseDTO> readMine(Long studentId) {
        return passReportRepository.findByStudent_IdOrderByCreatedAtDescIdDesc(studentId).stream()
                .map(PassReportResponseDTO::from)
                .toList();
    }

    /** 선생님: 내가 받은 합격 보고 */
    @Transactional(readOnly = true)
    public List<PassReportResponseDTO> readReceived(Long teacherId) {
        return passReportRepository.findByTeacher_IdOrderByCreatedAtDescIdDesc(teacherId).stream()
                .map(PassReportResponseDTO::from)
                .toList();
    }

    /** 선생님: 내가 받은 합격 보고 한 건. 다른 선생님이 받은 보고는 없는 것으로 본다. */
    @Transactional(readOnly = true)
    public PassReportResponseDTO readReceivedOne(Long reportId, Long teacherId) {
        return passReportRepository.findById(reportId)
                .filter(report -> report.getTeacher().getId().equals(teacherId))
                .map(PassReportResponseDTO::from)
                .orElseThrow(() -> PassReportException.notFound("합격 보고를 찾을 수 없습니다."));
    }

    private User findCareerTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> PassReportException.notFound("선생님을 찾을 수 없습니다."));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw PassReportException.badRequest("진로 선생님에게만 보낼 수 있습니다.");
        }
        return teacher;
    }

    /** 예) "3101 홍길동 학생이 카카오 서류에 합격했습니다. 면접일: 2026-11-02" */
    private String notificationContent(PassReportEntity report) {
        User student = report.getStudent();
        String content = student.getStudent_number() + " " + student.getName() + " 학생이 "
                + report.getCompanyName() + " " + report.getType().getLabel() + "에 합격했습니다.";
        if (report.getInterviewDate() != null) {
            content += " 면접일: " + report.getInterviewDate();
        }
        return content;
    }
}
