package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.banner.entity.BannerEntity;
import com.example.kodyjobdam.banner.repository.BannerRepository;
import com.example.kodyjobdam.banner.service.BannerImageStorage;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.repository.CommonWeeklyLockRepository;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.course.repository.CourseWeeklyLockRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.form.service.FormFileService;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.notice.entity.ReservationKind;
import com.example.kodyjobdam.notice.repository.CounselingReminderRepository;
import com.example.kodyjobdam.notification.repository.NotificationRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.passreport.repository.PassReportRepository;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.recruit.service.RecruitService;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountWithdrawalCleanupService {

    private final CounselingReservationCryptoService cryptoService;
    private final CommonRepository commonRepository;
    private final CourseRepository courseRepository;
    private final CommonWeeklyLockRepository commonWeeklyLockRepository;
    private final CourseWeeklyLockRepository courseWeeklyLockRepository;
    private final CounselingReminderRepository reminderRepository;
    private final FormSubmissionRepository submissionRepository;
    private final FormFileService formFileService;
    private final FormRepository formRepository;
    private final FormService formService;
    private final RecruitRepository recruitRepository;
    private final RecruitService recruitService;
    private final PassReportRepository passReportRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationExpirationService notificationExpirationService;
    private final BannerRepository bannerRepository;
    private final BannerImageStorage bannerImageStorage;

    @Transactional
    public void deleteRecords(User user) {
        Long userId = user.getId();
        String submitterHash = cryptoService.submitterHash(userId);
        List<String> targetUrls = new ArrayList<>();
        List<CanceledCounseling> canceledCounselings = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 신청자와 담당 선생님 양쪽을 조회한다. 같은 예약은 한 번만 지운다.
        var common = new LinkedHashMap<Long, CommonEntity>();
        commonRepository.findBySubmitterHash(submitterHash).forEach(reservation -> {
            common.put(reservation.getReservation_id(), reservation);
            if ((reservation.getState() == com.example.kodyjobdam.common.entity.StateEnum.WAITING
                    || reservation.getState() == com.example.kodyjobdam.common.entity.StateEnum.RESERVED)
                    && !reservation.getDate().isBefore(today)) {
                canceledCounselings.add(new CanceledCounseling(reservation.getTeacher(),
                        reservation.getReservation_id(), reservation.getDate(), reservation.getPeriod(),
                        "/teacher/common", NotificationType.COMMON_COUNSELING_CANCELED_BY_WITHDRAWAL));
            }
        });
        commonRepository.findByTeacher_Id(userId)
                .forEach(reservation -> common.put(reservation.getReservation_id(), reservation));
        var course = new LinkedHashMap<Long, CourseEntity>();
        courseRepository.findBySubmitterHash(submitterHash).forEach(reservation -> {
            course.put(reservation.getReservation_id(), reservation);
            if ((reservation.getState() == com.example.kodyjobdam.course.entity.StateEnum.WAITING
                    || reservation.getState() == com.example.kodyjobdam.course.entity.StateEnum.RESERVED)
                    && !reservation.getDate().isBefore(today)) {
                canceledCounselings.add(new CanceledCounseling(reservation.getTeacher(),
                        reservation.getReservation_id(), reservation.getDate(), reservation.getPeriod(),
                        "/teacher/course", NotificationType.COURSE_COUNSELING_CANCELED_BY_WITHDRAWAL));
            }
        });
        courseRepository.findByTeacher_Id(userId)
                .forEach(reservation -> course.put(reservation.getReservation_id(), reservation));

        if (!common.isEmpty()) {
            common.keySet().forEach(id -> {
                targetUrls.add("/teacher/common/" + id);
                targetUrls.add("/student/common/" + id);
            });
            reminderRepository.deleteByReservationKindAndReservationIdIn(ReservationKind.COMMON, common.keySet());
            commonRepository.deleteAll(common.values());
        }
        if (!course.isEmpty()) {
            course.keySet().forEach(id -> {
                targetUrls.add("/teacher/course/" + id);
                targetUrls.add("/student/course/" + id);
            });
            reminderRepository.deleteByReservationKindAndReservationIdIn(ReservationKind.COURSE, course.keySet());
            courseRepository.deleteAll(course.values());
        }
        commonWeeklyLockRepository.deleteByTeacher_Id(userId);
        courseWeeklyLockRepository.deleteByTeacher_Id(userId);

        // 답변을 먼저 지운 뒤 파일을 지워야 답변의 첨부 파일 참조가 남지 않는다.
        submissionRepository.deleteAll(submissionRepository.findByUser_Id(userId));
        submissionRepository.flush();
        formFileService.deleteAllByUser(userId);

        // 공고는 연결된 폼을 함께 지운다. 남은 독립 폼도 제출/첨부 파일과 함께 정리한다.
        var recruits = recruitRepository.findByUserIdOrderByCreatedAtDesc(userId);
        recruits.forEach(recruit -> {
            targetUrls.add("/recruit/" + recruit.getId());
            if (recruit.getFormId() != null) {
                targetUrls.add("/form/" + recruit.getFormId());
            }
            recruitService.delete(recruit.getId(), userId);
        });
        formRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(form -> {
            targetUrls.add("/form/" + form.getId());
            formService.deleteForRecruit(form.getId());
        });

        var reports = passReportRepository.findByStudent_IdOrderBySubmittedAtDescIdDesc(userId);
        reports.forEach(report -> targetUrls.add("/teacher/pass-report/" + report.getId()));
        passReportRepository.deleteAll(reports);
        if (!targetUrls.isEmpty()) {
            notificationRepository.deleteByTargetUrlIn(targetUrls);
        }
        notificationRepository.deleteByReceiver_Id(userId);
        // 예약 상세로 향하는 기존 알림을 지운 뒤, 목록으로 이동하는 취소 알림을 새로 남긴다.
        canceledCounselings.forEach(counseling -> notificationService.notifyUser(
                counseling.teacher(), counseling.type(),
                "상담 예약 취소",
                counseling.date() + " " + counseling.period() + " 상담 예약이 학생의 회원 탈퇴로 취소되었습니다.",
                counseling.reservationId(), counseling.targetUrl(),
                notificationExpirationService.counselingExpiresAt(counseling.date())));

        List<BannerEntity> banners = bannerRepository.findByUser_Id(userId);
        bannerRepository.deleteAll(banners);
        afterCommit(() -> banners.forEach(banner -> {
            try {
                bannerImageStorage.delete(banner.getImageUrl());
            } catch (RuntimeException e) {
                log.warn("탈퇴한 사용자의 배너 이미지 삭제 실패. imageUrl={}", banner.getImageUrl(), e);
            }
        }));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private record CanceledCounseling(User teacher, Long reservationId, LocalDate date,
                                      String period, String targetUrl, NotificationType type) {
    }
}
