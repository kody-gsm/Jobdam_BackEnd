package com.example.kodyjobdam.notification.controller;

import com.example.kodyjobdam.notification.dto.NotificationResponseDTO;
import com.example.kodyjobdam.notification.dto.SseTicketResponseDTO;
import com.example.kodyjobdam.notification.dto.UnreadCountResponseDTO;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.notification.service.NotificationSseService;
import com.example.kodyjobdam.notification.service.SseTicketService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final SseTicketService sseTicketService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public Page<NotificationResponseDTO> getNotifications(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationService.getNotifications(userId, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponseDTO getUnreadCount() {
        return notificationService.getUnreadCount(securityUtil.getCurrentUserId());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponseDTO markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId, securityUtil.getCurrentUserId());
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(securityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subscribe-ticket")
    public SseTicketResponseDTO issueSubscribeTicket() {
        return sseTicketService.issue(securityUtil.getCurrentUserId());
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String ticket) {
        Long userId = sseTicketService.consume(ticket);
        return notificationSseService.subscribe(userId);
    }
}
