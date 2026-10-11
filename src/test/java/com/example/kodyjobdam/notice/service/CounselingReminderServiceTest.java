package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.entity.ReservationKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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
        when(discordNoticeService.sendDirectMessage("123", "message")).thenReturn("sent-message");

        reminderService.sendIfDue(ReservationKind.COMMON, 5L, 2, now);

        InOrder order = inOrder(claimService, discordNoticeService);
        order.verify(claimService).claim(ReservationKind.COMMON, 5L, 2, now);
        order.verify(discordNoticeService).sendDirectMessage("123", "message");
        order.verify(claimService).markSent(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("token"), org.mockito.ArgumentMatchers.eq("123"),
                org.mockito.ArgumentMatchers.eq("sent-message"), any(LocalDateTime.class));
    }

    @Test
    void DM_실패시_선점을_풀어_다음_실행에서_재시도한다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 6, 40);
        when(claimService.claim(ReservationKind.COMMON, 5L, 2, now))
                .thenReturn(new CounselingReminderClaimService.Delivery(7L, "token", "123", "message"));
        when(discordNoticeService.sendDirectMessage("123", "message"))
                .thenThrow(new IllegalStateException("Discord unavailable"));

        assertThrows(IllegalStateException.class,
                () -> reminderService.sendIfDue(ReservationKind.COMMON, 5L, 2, now));

        verify(claimService).release(7L, "token");
        verify(claimService, never()).markSent(any(), any(), any(), any(), any());
    }

    @Test
    void 발송_24시간이_지난_DM만_삭제하고_결과를_기록한다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 10, 0);
        when(claimService.sentMessagesBefore(now.minusDays(1))).thenReturn(List.of(
                new CounselingReminderClaimService.SentMessage(7L, "123", "message-1")));

        reminderService.deleteExpiredMessages(now);

        InOrder order = inOrder(claimService, discordNoticeService);
        order.verify(claimService).sentMessagesBefore(now.minusDays(1));
        order.verify(discordNoticeService).deleteDirectMessage("123", "message-1");
        order.verify(claimService).markDeleted(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("message-1"), any(LocalDateTime.class));
    }

    @Test
    void DM_삭제_실패시_다음_실행에서_재시도할_수_있다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 10, 0);
        when(claimService.sentMessagesBefore(now.minusDays(1))).thenReturn(List.of(
                new CounselingReminderClaimService.SentMessage(7L, "123", "message-1")));
        org.mockito.Mockito.doThrow(new IllegalStateException("Discord unavailable"))
                .when(discordNoticeService).deleteDirectMessage("123", "message-1");

        reminderService.deleteExpiredMessages(now);

        verify(claimService, never()).markDeleted(any(), any(), any());
    }
}
