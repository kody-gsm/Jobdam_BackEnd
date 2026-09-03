package com.example.kodyjobdam.notification.repository;

import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-repository-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteExpiredNotificationsDeletesOnlyExpiredRowsAndReturnsCount() {
        User receiver = saveUser();
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 3, 0);

        notificationRepository.save(notification(receiver, NotificationType.RECRUIT_PUBLISHED, 1L, now.minusSeconds(1), false));
        notificationRepository.save(notification(receiver, NotificationType.FORM_PUBLISHED, 2L, now.plusSeconds(1), false));
        notificationRepository.save(notification(receiver, NotificationType.COUNSELING_APPROVED, 3L, now.minusDays(1), true));

        int deletedCount = notificationRepository.deleteExpiredNotifications(now);
        entityManager.flush();
        entityManager.clear();

        assertThat(deletedCount).isEqualTo(2);
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().getFirst().getTargetId()).isEqualTo(2L);
    }

    private User saveUser() {
        User user = User.builder()
                .name("사용자")
                .student_number("3001")
                .email("user@test.com")
                .role(UserRole.STUDENT)
                .emailVerified(true)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Notification notification(User receiver, NotificationType type, Long targetId,
                                      LocalDateTime expiresAt, boolean read) {
        return Notification.builder()
                .receiver(receiver)
                .type(type)
                .title("제목")
                .content("내용")
                .targetId(targetId)
                .targetUrl("/target/" + targetId)
                .read(read)
                .expiresAt(expiresAt)
                .build();
    }
}
