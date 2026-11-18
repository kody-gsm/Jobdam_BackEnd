package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.banner.service.BannerImageStorage;
import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.form.service.FormFileService;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.repository.NotificationRepository;
import com.example.kodyjobdam.recruit.service.RecruitService;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:withdrawal-records-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountWithdrawalCleanupService.class)
class AccountWithdrawalCleanupJpaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private AccountWithdrawalCleanupService cleanupService;
    @Autowired private CommonRepository commonRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private FormSubmissionRepository submissionRepository;
    @Autowired private NotificationRepository notificationRepository;

    @MockBean private CounselingReservationCryptoService cryptoService;
    @MockBean private FormFileService formFileService;
    @MockBean private FormService formService;
    @MockBean private RecruitService recruitService;
    @MockBean private BannerImageStorage bannerImageStorage;

    @Test
    void 탈퇴하면_상담과_폼_제출과_연결된_알림은_사라지고_다른_계정은_남는다() {
        User student = entityManager.merge(User.builder()
                .name("학생").email("student@gsm.hs.kr").role(UserRole.STUDENT).build());
        User teacher = entityManager.merge(User.builder()
                .name("선생님").email("teacher@gsm.hs.kr").role(UserRole.TEACHER).build());
        when(cryptoService.submitterHash(student.getId())).thenReturn("student-hash");

        CommonEntity common = CommonEntity.builder().teacher(teacher).submitterHash("student-hash")
                .date(LocalDate.of(2026, 12, 1)).period("1교시").build();
        CourseEntity course = CourseEntity.builder().teacher(teacher).submitterHash("student-hash")
                .date(LocalDate.of(2026, 12, 2)).period("2교시").build();
        entityManager.persist(common);
        entityManager.persist(course);

        FormEntity form = FormEntity.builder().user(teacher).title("지원서").build();
        entityManager.persist(form);
        entityManager.persist(FormSubmissionEntity.builder().form(form).user(student).build());
        entityManager.persist(Notification.builder().receiver(teacher)
                .type(NotificationType.COMMON_COUNSELING_REQUESTED).title("상담 신청")
                .content("신청되었습니다").targetId(common.getReservation_id())
                .targetUrl("/teacher/common/" + common.getReservation_id())
                .expiresAt(LocalDateTime.now().plusDays(1)).build());
        entityManager.flush();

        cleanupService.deleteRecords(student);
        entityManager.flush();
        entityManager.clear();

        assertThat(commonRepository.findBySubmitterHash("student-hash")).isEmpty();
        assertThat(courseRepository.findBySubmitterHash("student-hash")).isEmpty();
        assertThat(submissionRepository.findByUser_Id(student.getId())).isEmpty();
        assertThat(notificationRepository.findAll()).isEmpty();
        assertThat(entityManager.find(User.class, teacher.getId())).isNotNull();
        assertThat(entityManager.find(FormEntity.class, form.getId())).isNotNull();
    }
}
