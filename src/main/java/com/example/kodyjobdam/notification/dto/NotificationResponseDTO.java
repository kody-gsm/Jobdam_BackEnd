package com.example.kodyjobdam.notification.dto;

import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        @JsonIgnore Long receiverId,
        NotificationType type,
        String title,
        String content,
        Long targetId,
        String targetUrl,
        boolean isRead,
        String targetStatus,
        boolean expired,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {

    public static NotificationResponseDTO from(Notification notification) {
        return from(notification, null, false);
    }

    public static NotificationResponseDTO from(Notification notification, String targetStatus, boolean expired) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getReceiver().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetId(),
                notification.getTargetUrl(),
                notification.isRead(),
                targetStatus,
                expired,
                notification.getCreatedAt(),
                notification.getExpiresAt()
        );
    }
}
