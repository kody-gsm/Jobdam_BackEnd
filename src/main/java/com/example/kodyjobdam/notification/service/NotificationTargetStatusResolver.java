package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationTargetStatusResolver {

    private final RecruitRepository recruitRepository;
    private final FormRepository formRepository;
    private final CommonRepository commonRepository;
    private final CourseRepository courseRepository;
    private final NotificationExpirationService notificationExpirationService;

    public TargetStatus resolve(Notification notification) {
        NotificationType type = notification.getType();

        if (type == NotificationType.RECRUIT_PUBLISHED) {
            return recruitRepository.findById(notification.getTargetId())
                    .map(recruit -> new TargetStatus(
                            recruit.getStatus().name(),
                            notificationExpirationService.isRecruitDeadlineExpired(recruit.getDeadline())
                    ))
                    .orElse(TargetStatus.missing());
        }

        if (type == NotificationType.FORM_PUBLISHED) {
            return formRepository.findById(notification.getTargetId())
                    .map(form -> new TargetStatus(
                            form.getStatus().name(),
                            notificationExpirationService.isFormDeadlineExpired(form.getDeadline())
                    ))
                    .orElse(TargetStatus.missing());
        }

        if (type == NotificationType.COMMON_COUNSELING_REQUESTED) {
            return commonRepository.findById(notification.getTargetId())
                    .map(reservation -> new TargetStatus(
                            reservation.getState().name(),
                            notificationExpirationService.isCounselingDateExpired(reservation.getDate())
                    ))
                    .orElse(TargetStatus.missing());
        }

        if (type == NotificationType.COURSE_COUNSELING_REQUESTED) {
            return courseRepository.findById(notification.getTargetId())
                    .map(reservation -> new TargetStatus(
                            reservation.getState().name(),
                            notificationExpirationService.isCounselingDateExpired(reservation.getDate())
                    ))
                    .orElse(TargetStatus.missing());
        }

        if (type == NotificationType.COUNSELING_APPROVED || type == NotificationType.COUNSELING_REJECTED) {
            if (notification.getTargetUrl() != null && notification.getTargetUrl().contains("/course/")) {
                return courseRepository.findById(notification.getTargetId())
                        .map(reservation -> new TargetStatus(
                                reservation.getState().name(),
                                notificationExpirationService.isCounselingDateExpired(reservation.getDate())
                        ))
                        .orElse(TargetStatus.missing());
            }

            return commonRepository.findById(notification.getTargetId())
                    .map(reservation -> new TargetStatus(
                            reservation.getState().name(),
                            notificationExpirationService.isCounselingDateExpired(reservation.getDate())
                    ))
                    .orElse(TargetStatus.missing());
        }

        return TargetStatus.missing();
    }

    public record TargetStatus(String status, boolean expired) {
        public static TargetStatus missing() {
            return new TargetStatus(null, false);
        }
    }
}
