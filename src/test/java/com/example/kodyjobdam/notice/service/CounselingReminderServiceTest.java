package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.entity.ReservationKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounselingReminderServiceTest {

    @Mock CounselingReminderClaimService claimService;
    @Mock DiscordNoticeService discordNoticeService;
    @InjectMocks CounselingReminderService reminderService;

    @Test
    void DM_발송_후_성공을_기록한다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 6, 40);
        when(claimService.claim(ReservationKind.COMMON, 5L, 2, now))
                .thenReturn(new CounselingReminderClaimService.Delivery(7L, "token", "123", "message"));

        reminderService.sendIfDue(ReservationKind.COMMON, 5L, 2, now);

        InOrder order = inOrder(claimService, discordNoticeService);
        order.verify(claimService).claim(ReservationKind.COMMON, 5L, 2, now);
        order.verify(discordNoticeService).sendDirectMessage("123", "message");
        order.verify(claimService).markSent(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("token"), any(LocalDateTime.class));
    }

    @Test
    void DM_실패시_선점을_풀어_다음_실행에서_재시도한다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 6, 40);
        when(claimService.claim(ReservationKind.COMMON, 5L, 2, now))
                .thenReturn(new CounselingReminderClaimService.Delivery(7L, "token", "123", "message"));
        org.mockito.Mockito.doThrow(new IllegalStateException("Discord unavailable"))
                .when(discordNoticeService).sendDirectMessage("123", "message");

        assertThrows(IllegalStateException.class,
                () -> reminderService.sendIfDue(ReservationKind.COMMON, 5L, 2, now));

        verify(claimService).release(7L, "token");
        verify(claimService, never()).markSent(any(), any(), any());
    }
}
