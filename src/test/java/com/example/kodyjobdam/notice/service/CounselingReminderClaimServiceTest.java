package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.notice.entity.CounselingReminder;
import com.example.kodyjobdam.notice.entity.ReservationKind;
import com.example.kodyjobdam.notice.repository.CounselingReminderRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounselingReminderClaimServiceTest {

    @Mock CommonRepository commonRepository;
    @Mock CourseRepository courseRepository;
    @Mock CounselingReminderRepository reminderRepository;
    @Mock UserRepository userRepository;
    @Mock CounselingReservationCryptoService cryptoService;
    @InjectMocks CounselingReminderClaimService claimService;

    @Test
    void 시작_두_시간_전_알림을_5분_이후에도_재시도할_수_있다() {
        prepareReservedStudent();
        when(reminderRepository.saveAndFlush(any(CounselingReminder.class))).thenAnswer(invocation -> {
            CounselingReminder reminder = invocation.getArgument(0);
            ReflectionTestUtils.setField(reminder, "id", 7L);
            return reminder;
        });

        CounselingReminderClaimService.Delivery delivery = claimService.claim(
                ReservationKind.COMMON, 5L, 2, LocalDateTime.of(2026, 10, 6, 6, 50));

        assertNotNull(delivery);
        assertEquals(7L, delivery.reminderId());
        assertEquals("123456789", delivery.discordUserId());
        assertEquals("상담 일정 알림입니다. 2026-10-06 1교시 일반 상담 일정이 있습니다.", delivery.message());
    }

    @Test
    void 두_시간_전_알림은_한_시간_전_시점부터_보내지_않는다() {
        when(commonRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(reservation(StateEnum.RESERVED)));

        assertNull(claimService.claim(ReservationKind.COMMON, 5L, 2,
                LocalDateTime.of(2026, 10, 6, 7, 40)));
        verify(reminderRepository, never()).saveAndFlush(any());
    }

    @Test
    void 한_시간_전_알림은_상담_시작_전까지_재시도한다() {
        prepareReservedStudent();
        when(reminderRepository.saveAndFlush(any(CounselingReminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(claimService.claim(ReservationKind.COMMON, 5L, 1,
                LocalDateTime.of(2026, 10, 6, 8, 20)));
    }

    @Test
    void 취소된_상담에는_선점을_만들지_않는다() {
        when(commonRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(reservation(StateEnum.CANCEL)));

        assertNull(claimService.claim(ReservationKind.COMMON, 5L, 2,
                LocalDateTime.of(2026, 10, 6, 6, 40)));
        verify(reminderRepository, never()).saveAndFlush(any());
    }

    @Test
    void 선점이_만료되면_재선점하고_이전_시도의_완료는_무시한다() {
        prepareReservedStudent();
        CounselingReminder reminder = new CounselingReminder(ReservationKind.COMMON, 5L,
                LocalDateTime.of(2026, 10, 6, 8, 40), 2);
        ReflectionTestUtils.setField(reminder, "id", 7L);
        reminder.claim("old-token", LocalDateTime.of(2026, 10, 6, 6, 40));
        when(reminderRepository.findBySlot(ReservationKind.COMMON, 5L,
                LocalDateTime.of(2026, 10, 6, 8, 40), 2)).thenReturn(Optional.of(reminder));
        when(reminderRepository.saveAndFlush(reminder)).thenReturn(reminder);
        when(reminderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(reminder));

        CounselingReminderClaimService.Delivery delivery = claimService.claim(
                ReservationKind.COMMON, 5L, 2, LocalDateTime.of(2026, 10, 6, 6, 51));
        assertNotNull(delivery);
        claimService.markSent(7L, "old-token", LocalDateTime.of(2026, 10, 6, 6, 52));
        assertNull(reminder.getSentAt());
        claimService.markSent(7L, delivery.token(), LocalDateTime.of(2026, 10, 6, 6, 52));
        assertNotNull(reminder.getSentAt());
    }

    private void prepareReservedStudent() {
        when(commonRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(reservation(StateEnum.RESERVED)));
        when(cryptoService.decrypt("encrypted-id")).thenReturn("10");
        when(cryptoService.submitterHash(10L)).thenReturn("student-hash");
        when(userRepository.findById(10L)).thenReturn(Optional.of(
                User.builder().id(10L).discordUserId("123456789").build()));
    }

    private CommonEntity reservation(StateEnum state) {
        return CommonEntity.builder().reservation_id(5L).date(LocalDate.of(2026, 10, 6))
                .period("1교시").state(state).encryptedUserId("encrypted-id")
                .submitterHash("student-hash").build();
    }
}
