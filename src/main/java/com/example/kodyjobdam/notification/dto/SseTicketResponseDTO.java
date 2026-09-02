package com.example.kodyjobdam.notification.dto;

import java.time.LocalDateTime;

public record SseTicketResponseDTO(
        String ticket,
        LocalDateTime expiresAt
) {
}
