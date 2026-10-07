package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.entity.ReservationKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CounselingReminderService {

    private final CounselingReminderClaimService claimService;
    private final DiscordNoticeService discordNoticeService;

    public List<Long> commonReservationIds(LocalDate date) {
        return claimService.commonReservationIds(date);
    }

    public List<Long> courseReservationIds(LocalDate date) {
        return claimService.courseReservationIds(date);
    }

    public void sendIfDue(ReservationKind kind, Long reservationId, int hoursBefore, LocalDateTime now) {
        CounselingReminderClaimService.Delivery delivery = claimService.claim(kind, reservationId, hoursBefore, now);
        if (delivery == null) return;

        try {
            discordNoticeService.sendDirectMessage(delivery.discordUserId(), delivery.message());
        } catch (RuntimeException e) {
            claimService.release(delivery.reminderId(), delivery.token());
            throw e;
        }
        claimService.markSent(delivery.reminderId(), delivery.token(), LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
}
