package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitServiceTest {

    @Mock
    private RecruitRepository recruitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.example.kodyjobdam.notification.service.NotificationExpirationService notificationExpirationService;

    @InjectMocks
    private RecruitService recruitService;

    @Test
    void publishCreatesStudentNotifications() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .deadline("2026-09-10")
                .status(RecruitStatus.DRAFT)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));
        when(notificationExpirationService.recruitExpiresAt("2026-09-10"))
                .thenReturn(LocalDateTime.of(2026, 10, 10, 23, 59, 59));

        recruitService.publish(10L);

        verify(notificationService).notifyAllStudents(
                eq(NotificationType.RECRUIT_PUBLISHED),
                eq("새로운 취업 공지"),
                eq("잡담 취업 공지가 게시되었습니다."),
                eq(10L),
                eq("/recruit/10"),
                eq(LocalDateTime.of(2026, 10, 10, 23, 59, 59))
        );
    }

    @Test
    void publishAlreadyPublishedRecruitDoesNotCreateDuplicateNotification() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(10L)
                .user(user(2L))
                .companyName("잡담")
                .status(RecruitStatus.PUBLISHED)
                .build();
        when(recruitRepository.findById(10L)).thenReturn(Optional.of(recruit));

        assertThatThrownBy(() -> recruitService.publish(10L))
                .isInstanceOf(RecruitException.class);
        verify(notificationService, never()).notifyAllStudents(any(), any(), any(), any(), any(), any());
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("선생님")
                .student_number("T" + id)
                .email("teacher" + id + "@test.com")
                .role(UserRole.TEACHER)
                .emailVerified(true)
                .build();
    }
}
