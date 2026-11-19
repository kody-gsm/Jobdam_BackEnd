package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationTargetStatusResolverWithdrawalTest {

    @Mock private RecruitRepository recruitRepository;
    @Mock private FormRepository formRepository;
    @Mock private CommonRepository commonRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private NotificationExpirationService expirationService;

    @InjectMocks private NotificationTargetStatusResolver resolver;

    @Test
    void 탈퇴로_취소된_상담_알림은_삭제된_예약을_조회하지_않고_취소_상태로_표시한다() {
        Notification common = Notification.builder().id(1L)
                .type(NotificationType.COMMON_COUNSELING_CANCELED_BY_WITHDRAWAL)
                .targetId(10L).targetUrl("/teacher/common").build();
        Notification course = Notification.builder().id(2L)
                .type(NotificationType.COURSE_COUNSELING_CANCELED_BY_WITHDRAWAL)
                .targetId(20L).targetUrl("/teacher/course").build();

        var statuses = resolver.resolveAll(List.of(common, course));

        assertThat(statuses.get(1L).status()).isEqualTo("CANCEL");
        assertThat(statuses.get(2L).status()).isEqualTo("CANCEL");
        assertThat(statuses.get(1L).expired()).isFalse();
        assertThat(statuses.get(2L).expired()).isFalse();
        verifyNoInteractions(commonRepository, courseRepository);
    }
}
