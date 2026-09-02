package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.common.exception.NotificationException;
import com.example.kodyjobdam.notification.dto.NotificationResponseDTO;
import com.example.kodyjobdam.notification.dto.UnreadCountResponseDTO;
import com.example.kodyjobdam.notification.entity.Notification;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.repository.NotificationRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseService notificationSseService;
    private final NotificationTargetStatusResolver notificationTargetStatusResolver;
    private final NotificationExpirationService notificationExpirationService;

    @Transactional
    public List<NotificationResponseDTO> notifyAllStudents(NotificationType type, String title, String content,
                                                           Long targetId, String targetUrl, LocalDateTime expiresAt) {
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        List<Notification> notifications = new ArrayList<>();

        for (User student : students) {
            if (!notificationRepository.existsByReceiver_IdAndTypeAndTargetId(student.getId(), type, targetId)) {
                notifications.add(createNotification(student, type, title, content, targetId, targetUrl, expiresAt));
            }
        }

        List<NotificationResponseDTO> responses = notificationRepository.saveAll(notifications).stream()
                .map(NotificationResponseDTO::from)
                .toList();

        sendAfterCommit(responses);
        return responses;
    }

    @Transactional
    public NotificationResponseDTO notifyUser(User receiver, NotificationType type, String title, String content,
                                              Long targetId, String targetUrl, LocalDateTime expiresAt) {
        if (notificationRepository.existsByReceiver_IdAndTypeAndTargetId(receiver.getId(), type, targetId)) {
            return null;
        }

        NotificationResponseDTO response = NotificationResponseDTO.from(notificationRepository.save(
                createNotification(receiver, type, title, content, targetId, targetUrl, expiresAt)
        ));

        sendAfterCommit(List.of(response));
        return response;
    }

    public Page<NotificationResponseDTO> getNotifications(Long receiverId, Pageable pageable) {
        return notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(receiverId, pageable)
                .map(this::toResponse);
    }

    public UnreadCountResponseDTO getUnreadCount(Long receiverId) {
        return new UnreadCountResponseDTO(notificationRepository.countByReceiver_IdAndReadFalse(receiverId));
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId, Long receiverId) {
        Notification notification = notificationRepository.findByIdAndReceiver_Id(notificationId, receiverId)
                .orElseThrow(() -> NotificationException.notFound("알림을 찾을 수 없습니다."));

        notification.markAsRead();
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long receiverId) {
        notificationRepository.markAllAsRead(receiverId);
    }

    private Notification createNotification(User receiver, NotificationType type, String title, String content,
                                            Long targetId, String targetUrl, LocalDateTime expiresAt) {
        return Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .content(content)
                .targetId(targetId)
                .targetUrl(targetUrl)
                .expiresAt(expiresAt == null ? notificationExpirationService.defaultExpiresAt() : expiresAt)
                .build();
    }

    private NotificationResponseDTO toResponse(Notification notification) {
        NotificationTargetStatusResolver.TargetStatus targetStatus =
                notificationTargetStatusResolver.resolve(notification);
        return NotificationResponseDTO.from(notification, targetStatus.status(), targetStatus.expired());
    }

    private void sendAfterCommit(List<NotificationResponseDTO> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Runnable send = () -> notifications.forEach(notification -> {
            try {
                notificationSseService.send(notification.receiverId(), notification);
            } catch (RuntimeException e) {
                log.debug("알림 SSE 전송 실패를 무시합니다. notificationId={}", notification.id(), e);
            }
        });

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
}
