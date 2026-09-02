package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.exception.NotificationException;
import com.example.kodyjobdam.notification.dto.SseTicketResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseTicketService {

    private final Map<String, SseTicket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.notification.sse-ticket-expiration-seconds:60}")
    private long expirationSeconds;

    public SseTicketResponseDTO issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationSeconds);
        tickets.put(ticket, new SseTicket(userId, expiresAt));

        return new SseTicketResponseDTO(ticket, expiresAt);
    }

    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw NotificationException.badRequest("SSE 연결 티켓이 필요합니다.");
        }

        SseTicket sseTicket = tickets.remove(ticket);
        if (sseTicket == null || sseTicket.expiresAt().isBefore(LocalDateTime.now())) {
            throw NotificationException.badRequest("SSE 연결 티켓이 올바르지 않거나 만료되었습니다.");
        }

        return sseTicket.userId();
    }

    private record SseTicket(Long userId, LocalDateTime expiresAt) {
    }
}
