package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.exception.NotificationException;
import com.example.kodyjobdam.notification.dto.NotificationResponseDTO;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.repository.NotificationRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private NotificationTargetStatusResolver notificationTargetStatusResolver;

    @Mock
    private NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyAllStudentsSavesOnlyStudentNotifications() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 10, 0, 0);
        User student1 = user(1L, UserRole.STUDENT);
        User student2 = user(2L, UserRole.STUDENT);
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student1, student2));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyAllStudents(
                NotificationType.RECRUIT_PUBLISHED,
                "새로운 취업 공지",
                "회사 취업 공지가 게시되었습니다.",
                10L,
                "/recruit/10",
                expiresAt
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(Notification::getExpiresAt)
                .containsOnly(expiresAt);
        assertThat(captor.getValue()).extracting(notification -> notification.getReceiver().getId())
                .containsExactly(1L, 2L);
        verify(notificationSseService).send(eq(1L), any(NotificationResponseDTO.class));
        verify(notificationSseService).send(eq(2L), any(NotificationResponseDTO.class));
    }

    @Test
    void notifyAllStudentsSkipsAlreadyCreatedNotification() {
        User student1 = user(1L, UserRole.STUDENT);
        User student2 = user(2L, UserRole.STUDENT);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 10, 0, 0);
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student1, student2));
        when(notificationRepository.existsByReceiver_IdAndTypeAndTargetId(1L, NotificationType.FORM_PUBLISHED, 20L))
                .thenReturn(true);
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyAllStudents(
                NotificationType.FORM_PUBLISHED,
                "새로운 폼",
                "설문 폼이 게시되었습니다.",
                20L,
                "/form/20",
                expiresAt
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getReceiver().getId()).isEqualTo(2L);
    }

    @Test
    void getNotificationsReadsOnlyCurrentUserNotifications() {
        User receiver = user(1L, UserRole.STUDENT);
        Notification notification = notification(receiver, 10L);
        PageRequest pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationTargetStatusResolver.resolve(notification))
                .thenReturn(new NotificationTargetStatusResolver.TargetStatus("PUBLISHED", false));

        assertThat(notificationService.getNotifications(1L, pageable).getContent()).hasSize(1);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void markAsReadRejectsOtherUsersNotification() {
        when(notificationRepository.findByIdAndReceiver_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, 1L))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void markAsReadUpdatesReadFlag() {
        Notification notification = notification(user(1L, UserRole.STUDENT), 10L);
        when(notificationRepository.findByIdAndReceiver_Id(10L, 1L)).thenReturn(Optional.of(notification));
        when(notificationTargetStatusResolver.resolve(notification))
                .thenReturn(new NotificationTargetStatusResolver.TargetStatus("PUBLISHED", false));

        NotificationResponseDTO response = notificationService.markAsRead(10L, 1L);

        assertThat(response.isRead()).isTrue();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void unreadCountReturnsRepositoryCount() {
        when(notificationRepository.countByReceiver_IdAndReadFalse(1L)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(1L).unreadCount()).isEqualTo(3L);
    }

    @Test
    void sseFailureDoesNotFailNotificationSave() {
        User teacher = user(2L, UserRole.TEACHER);
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("broken emitter")).when(notificationSseService)
                .send(eq(2L), any(NotificationResponseDTO.class));

        NotificationResponseDTO response = notificationService.notifyUser(
                teacher,
                NotificationType.COMMON_COUNSELING_REQUESTED,
                "새로운 상담 신청",
                "학생이 상담을 신청했습니다.",
                1L,
                "/teacher/common/1",
                LocalDateTime.of(2026, 12, 9, 0, 0)
        );

        assertThat(response.receiverId()).isEqualTo(2L);
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .name("사용자" + id)
                .student_number("300" + id)
                .email("user" + id + "@test.com")
                .role(role)
                .emailVerified(true)
                .build();
    }

    private Notification notification(User receiver, Long id) {
        return Notification.builder()
                .id(id)
                .receiver(receiver)
                .type(NotificationType.RECRUIT_PUBLISHED)
                .title("제목")
                .content("내용")
                .targetId(100L)
                .targetUrl("/recruit/100")
                .expiresAt(LocalDateTime.of(2026, 12, 1, 0, 0))
                .build();
    }
}
