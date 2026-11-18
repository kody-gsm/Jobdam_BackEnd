package com.example.kodyjobdam.notification.service;

import com.example.kodyjobdam.notification.dto.NotificationResponseDTO;
import com.example.kodyjobdam.notification.dto.ReservationRealtimeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationSseService {

    private static final long TIMEOUT_MILLIS = 60L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        sendToEmitter(userId, emitter, "connect", "connected");
        return emitter;
    }

    public void send(Long receiverId, NotificationResponseDTO notification) {
        List<SseEmitter> userEmitters = emitters.get(receiverId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            sendToEmitter(receiverId, emitter, "notification", notification);
        }
    }

    public void sendReservation(Long receiverId, ReservationRealtimeEvent event) {
        List<SseEmitter> userEmitters = emitters.get(receiverId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            sendToEmitter(receiverId, emitter, "reservation", event);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.sse-heartbeat-millis:30000}")
    public void sendHeartbeat() {
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                sendToEmitter(userId, emitter, "heartbeat", "ping");
            }
        }
    }

    /**
     * 배포로 이 컨테이너가 내려갈 때 열린 SSE 연결을 먼저 닫는다.
     *
     * ContextClosedEvent는 graceful shutdown(웹 서버가 진행 중인 요청을 기다리는 단계)보다 먼저 온다.
     * 닫지 않으면 최대 1시간짜리 SSE 요청 때문에 graceful shutdown이 타임아웃까지 기다렸다가 강제로 끊긴다.
     * 먼저 닫아 주면 프론트가 바로 다시 연결하고, 그때는 프록시가 이미 새 컨테이너를 가리키고 있다.
     */
    @EventListener(ContextClosedEvent.class)
    public void closeAllOnShutdown() {
        int closed = 0;
        for (List<SseEmitter> userEmitters : emitters.values()) {
            for (SseEmitter emitter : userEmitters) {
                completeEmitter(emitter);
                closed++;
            }
        }
        emitters.clear();
        log.info("종료 전에 SSE 연결 {}개를 닫았습니다.", closed);
    }

    private void sendToEmitter(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 전송에 실패하여 연결을 정리합니다. userId={}, event={}", userId, eventName);
            removeEmitter(userId, emitter);
            completeEmitter(emitter);
        }
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
