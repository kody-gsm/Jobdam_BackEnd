package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.service.FormSubmissionService;
import com.example.kodyjobdam.notification.dto.NotificationResponseDTO;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 건수가 늘어도 쿼리 수가 함께 늘지 않는지 고정한다. */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-query-count-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationService.class, NotificationTargetStatusResolver.class, NotificationExpirationService.class,
        FormSubmissionService.class})
class NotificationQueryCountTest {

    private static final int STUDENTS = 30;
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "createdAt");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FormSubmissionService formSubmissionService;

    @MockBean
    private NotificationSseService notificationSseService;

    private Statistics statistics;
    private User teacher;
    private List<User> students;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        teacher = save(user("teacher", "T000", UserRole.TEACHER));
        students = new ArrayList<>();
        for (int i = 0; i < STUDENTS; i++) {
            students.add(save(user("student" + i, String.format("%04d", i), UserRole.STUDENT)));
        }
    }

    @Test
    void 폼_제출_목록은_제출자_수와_관계없이_쿼리_두_번으로_읽는다() {
        FormEntity form = save(FormEntity.builder().user(teacher).title("폼").status(FormStatus.PUBLISHED).build());
        students.forEach(student -> save(FormSubmissionEntity.builder().form(form).user(student).build()));
        flushAndClear();

        statistics.clear();
        int size = formSubmissionService.listSubmissions(form.getId(), teacher.getId()).size();

        assertThat(size).isEqualTo(STUDENTS);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void 전체_학생_알림의_중복_확인은_학생_수와_관계없이_한_번이다() {
        statistics.clear();
        List<NotificationResponseDTO> first = notify(20L);
        flushAndClear();

        // 학생 조회 1 + 중복 확인 1 + 저장 N
        assertThat(first).hasSize(STUDENTS);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L + STUDENTS);

        statistics.clear();
        List<NotificationResponseDTO> second = notify(20L);

        assertThat(second).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void 일부_학생만_받은_알림은_받지_않은_학생에게만_보낸다() {
        User received = students.get(0);
        save(notification(received, NotificationType.FORM_PUBLISHED, 20L, "/form/20"));
        flushAndClear();

        List<NotificationResponseDTO> responses = notify(20L);

        assertThat(responses).hasSize(STUDENTS - 1);
        assertThat(responses).extracting(NotificationResponseDTO::receiverId).doesNotContain(received.getId());
    }

    @Test
    void 알림_목록은_건수가_늘어도_쿼리_수가_같고_대상_상태를_종류별로_돌려준다() {
        User receiver = students.get(0);
        long firstRecruitId = 0;
        long firstCourseId = 0;
        for (int i = 0; i < 10; i++) {
            RecruitEntity recruit = save(RecruitEntity.builder().user(teacher).companyName("회사" + i)
                    .status(RecruitStatus.PUBLISHED).build());
            FormEntity form = save(FormEntity.builder().user(teacher).title("폼" + i).status(FormStatus.CLOSED).build());
            CommonEntity common = save(CommonEntity.builder().date(LocalDate.of(2099, 1, 1)).period("1교시")
                    .teacher(teacher).state(StateEnum.RESERVED).build());
            CourseEntity course = save(CourseEntity.builder().date(LocalDate.of(2000, 1, 1)).period("1교시")
                    .teacher(teacher).state(com.example.kodyjobdam.course.entity.StateEnum.CANCEL).build());
            firstRecruitId = i == 0 ? recruit.getId() : firstRecruitId;
            firstCourseId = i == 0 ? course.getReservation_id() : firstCourseId;

            save(notification(receiver, NotificationType.RECRUIT_PUBLISHED, recruit.getId(), "/recruit/" + i));
            save(notification(receiver, NotificationType.FORM_PUBLISHED, form.getId(), "/form/" + i));
            save(notification(receiver, NotificationType.COUNSELING_APPROVED, common.getReservation_id(), "/student/common"));
            save(notification(receiver, NotificationType.COUNSELING_REJECTED, course.getReservation_id(), "/student/course/x"));
        }
        // 대상이 지워진 알림과 대상이 없는 종류는 상태 없이 나간다.
        save(notification(receiver, NotificationType.RECRUIT_PUBLISHED, 999_999L, "/recruit/gone"));
        save(notification(receiver, NotificationType.PASS_REPORTED, 1L, "/teacher/pass-report/1"));
        flushAndClear();

        statistics.clear();
        notificationService.getNotifications(receiver.getId(), PageRequest.of(0, 8, NEWEST));
        long smallPage = statistics.getPrepareStatementCount();
        entityManager.clear();

        statistics.clear();
        List<NotificationResponseDTO> all = notificationService
                .getNotifications(receiver.getId(), PageRequest.of(0, 42, NEWEST)).getContent();
        long largePage = statistics.getPrepareStatementCount();

        assertThat(largePage).isLessThanOrEqualTo(smallPage + 1);
        assertThat(all).hasSize(42);
        long recruitId = firstRecruitId;
        long courseId = firstCourseId;
        assertThat(all).filteredOn(n -> n.type() == NotificationType.RECRUIT_PUBLISHED && n.targetId() == recruitId)
                .singleElement().extracting(NotificationResponseDTO::targetStatus).isEqualTo("PUBLISHED");
        assertThat(all).filteredOn(n -> n.type() == NotificationType.COUNSELING_REJECTED && n.targetId() == courseId)
                .singleElement().satisfies(n -> {
                    assertThat(n.targetStatus()).isEqualTo("CANCEL");
                    assertThat(n.expired()).isTrue();
                });
        assertThat(all).filteredOn(n -> n.type() == NotificationType.COUNSELING_APPROVED)
                .extracting(NotificationResponseDTO::targetStatus).containsOnly("RESERVED");
        assertThat(all).filteredOn(n -> n.targetId() == 999_999L || n.type() == NotificationType.PASS_REPORTED)
                .hasSize(2).extracting(NotificationResponseDTO::targetStatus).containsOnlyNulls();
    }

    private List<NotificationResponseDTO> notify(Long targetId) {
        return notificationService.notifyAllStudents(
                NotificationType.FORM_PUBLISHED, "제목", "내용", targetId, "/form/" + targetId, EXPIRES_AT);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private <T> T save(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private Notification notification(User receiver, NotificationType type, Long targetId, String targetUrl) {
        return Notification.builder().receiver(receiver).type(type).title("제목").content("내용")
                .targetId(targetId).targetUrl(targetUrl).expiresAt(EXPIRES_AT).build();
    }

    private User user(String name, String number, UserRole role) {
        return User.builder().name(name).student_number(number).email(name + "@gsm.hs.kr").role(role)
                .emailVerified(true).build();
    }
}
