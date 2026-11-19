package com.example.kodyjobdam.user.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalCleanupServiceTest {

    @Mock private CounselingReservationCryptoService cryptoService;
    @Mock private CommonRepository commonRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CommonWeeklyLockRepository commonWeeklyLockRepository;
    @Mock private CourseWeeklyLockRepository courseWeeklyLockRepository;
    @Mock private CounselingReminderRepository reminderRepository;
    @Mock private FormSubmissionRepository submissionRepository;
    @Mock private FormFileService formFileService;
    @Mock private FormRepository formRepository;
    @Mock private FormService formService;
    @Mock private RecruitRepository recruitRepository;
    @Mock private RecruitService recruitService;
    @Mock private PassReportRepository passReportRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationExpirationService notificationExpirationService;
    @Mock private BannerRepository bannerRepository;
    @Mock private BannerImageStorage bannerImageStorage;

    @InjectMocks private AccountWithdrawalCleanupService service;

    @Test
    void 탈퇴하면_일반_진로_상담과_알림_예약을_모두_제거한다() {
        User user = User.builder().id(10L).build();
        User teacher = User.builder().id(20L).build();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        CommonEntity common = CommonEntity.builder().reservation_id(100L).teacher(teacher)
                .date(date).period("1교시").state(com.example.kodyjobdam.common.entity.StateEnum.WAITING).build();
        CourseEntity course = CourseEntity.builder().reservation_id(200L).teacher(teacher)
                .date(date).period("2교시").state(com.example.kodyjobdam.course.entity.StateEnum.RESERVED).build();
        when(cryptoService.submitterHash(10L)).thenReturn("user-hash");
        when(commonRepository.findBySubmitterHash("user-hash")).thenReturn(List.of(common));
        when(commonRepository.findByTeacher_Id(10L)).thenReturn(List.of(common));
        when(courseRepository.findBySubmitterHash("user-hash")).thenReturn(List.of(course));

        service.deleteRecords(user);

        verify(reminderRepository).deleteByReservationKindAndReservationIdIn(ReservationKind.COMMON, Set.of(100L));
        verify(reminderRepository).deleteByReservationKindAndReservationIdIn(ReservationKind.COURSE, Set.of(200L));
        verify(commonRepository).deleteAll(argThat(records -> {
            var iterator = records.iterator();
            return iterator.hasNext() && iterator.next() == common && !iterator.hasNext();
        }));
        verify(courseRepository).deleteAll(argThat(records -> records.iterator().next() == course));
        verify(notificationRepository).deleteByTargetUrlIn(argThat(urls ->
                urls.containsAll(List.of("/teacher/common/100", "/student/common/100",
                        "/teacher/course/200", "/student/course/200"))));
        verify(notificationRepository).deleteByReceiver_Id(10L);
        verify(notificationService).notifyUser(teacher, NotificationType.COMMON_COUNSELING_CANCELED_BY_WITHDRAWAL,
                "상담 예약 취소", date + " 1교시 상담 예약이 학생의 회원 탈퇴로 취소되었습니다.",
                100L, "/teacher/common", null);
        verify(notificationService).notifyUser(teacher, NotificationType.COURSE_COUNSELING_CANCELED_BY_WITHDRAWAL,
                "상담 예약 취소", date + " 2교시 상담 예약이 학생의 회원 탈퇴로 취소되었습니다.",
                200L, "/teacher/course", null);
        verify(formFileService).deleteAllByUser(10L);
    }
}
