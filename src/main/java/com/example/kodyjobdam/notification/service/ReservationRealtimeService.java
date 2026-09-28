package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.notification.dto.ReservationRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRealtimeService {

    private final NotificationSseService notificationSseService;

    public void sendAfterCommit(Collection<Long> receiverIds, ReservationRealtimeEvent event) {
        Runnable send = () -> receiverIds.stream()
                .filter(receiverId -> receiverId != null)
                .distinct()
                .forEach(receiverId -> sendQuietly(receiverId, event));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }

        send.run();
    }

    private void sendQuietly(Long receiverId, ReservationRealtimeEvent event) {
        try {
            notificationSseService.sendReservation(receiverId, event);
        } catch (RuntimeException e) {
            log.debug("상담 실시간 SSE 전송 실패를 무시합니다. receiverId={}, reservationId={}",
                    receiverId, event.reservationId(), e);
        }
    }
}
