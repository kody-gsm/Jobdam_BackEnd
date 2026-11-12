package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationTargetStatusResolver {

    private final RecruitRepository recruitRepository;
    private final FormRepository formRepository;
    private final CommonRepository commonRepository;
    private final CourseRepository courseRepository;
    private final NotificationExpirationService notificationExpirationService;

    public TargetStatus resolve(Notification notification) {
        return resolveAll(List.of(notification)).get(notification.getId());
    }

    /** 알림 ID별 대상 상태. 알림 수와 관계없이 대상 테이블마다 한 번씩만 읽는다. */
    public Map<Long, TargetStatus> resolveAll(List<Notification> notifications) {
        Map<TargetKind, Set<Long>> targetIds = new EnumMap<>(TargetKind.class);
        for (Notification notification : notifications) {
            TargetKind kind = kindOf(notification);
            if (kind != null) {
                targetIds.computeIfAbsent(kind, ignored -> new HashSet<>()).add(notification.getTargetId());
            }
        }

        Map<TargetKind, Map<Long, TargetStatus>> loaded = new EnumMap<>(TargetKind.class);
        targetIds.forEach((kind, ids) -> loaded.put(kind, load(kind, ids)));

        Map<Long, TargetStatus> result = new HashMap<>();
        for (Notification notification : notifications) {
            Map<Long, TargetStatus> statuses = loaded.getOrDefault(kindOf(notification), Map.of());
            result.put(notification.getId(), statuses.getOrDefault(notification.getTargetId(), TargetStatus.missing()));
        }
        return result;
    }

    private TargetKind kindOf(Notification notification) {
        NotificationType type = notification.getType();

        if (type == NotificationType.RECRUIT_PUBLISHED) {
            return TargetKind.RECRUIT;
        }
        if (type == NotificationType.FORM_PUBLISHED) {
            return TargetKind.FORM;
        }
        if (type == NotificationType.COMMON_COUNSELING_REQUESTED) {
            return TargetKind.COMMON;
        }
        if (type == NotificationType.COURSE_COUNSELING_REQUESTED) {
            return TargetKind.COURSE;
        }
        if (type == NotificationType.COUNSELING_APPROVED
                || type == NotificationType.COUNSELING_REJECTED
                || type == NotificationType.COUNSELING_AUTO_CANCELED
                || type == NotificationType.COUNSELING_CANCELED_BY_STUDENT
                || type == NotificationType.COUNSELING_EXPIRED) {
            String targetUrl = notification.getTargetUrl();
            return targetUrl != null && targetUrl.contains("/course/") ? TargetKind.COURSE : TargetKind.COMMON;
        }
        return null;
    }

    private Map<Long, TargetStatus> load(TargetKind kind, Set<Long> ids) {
        Map<Long, TargetStatus> statuses = new HashMap<>();
        switch (kind) {
            case RECRUIT -> {
                for (RecruitEntity recruit : recruitRepository.findAllById(ids)) {
                    statuses.put(recruit.getId(), new TargetStatus(
                            recruit.getStatus().name(),
                            notificationExpirationService.isRecruitDeadlineExpired(recruit.getDeadline())));
                }
            }
            case FORM -> {
                for (FormEntity form : formRepository.findAllById(ids)) {
                    statuses.put(form.getId(), new TargetStatus(
                            form.getStatus().name(),
                            notificationExpirationService.isFormDeadlineExpired(form.getDeadline())));
                }
            }
            case COMMON -> {
                for (CommonEntity reservation : commonRepository.findAllById(ids)) {
                    statuses.put(reservation.getReservation_id(), new TargetStatus(
                            reservation.getState().name(),
                            notificationExpirationService.isCounselingDateExpired(reservation.getDate())));
                }
            }
            case COURSE -> {
                for (CourseEntity reservation : courseRepository.findAllById(ids)) {
                    statuses.put(reservation.getReservation_id(), new TargetStatus(
                            reservation.getState().name(),
                            notificationExpirationService.isCounselingDateExpired(reservation.getDate())));
                }
            }
        }
        return statuses;
    }

    private enum TargetKind {
        RECRUIT, FORM, COMMON, COURSE
    }

    public record TargetStatus(String status, boolean expired) {
        public static TargetStatus missing() {
            return new TargetStatus(null, false);
        }
    }
}
