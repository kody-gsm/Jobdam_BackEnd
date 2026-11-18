package com.example.kodyjobdam.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationSseServiceTest {

    private final NotificationSseService service = new NotificationSseService();

    @Test
    void 종료할_때_열린_연결을_모두_닫는다() {
        SseEmitter first = service.subscribe(1L);
        SseEmitter second = service.subscribe(1L);
        SseEmitter other = service.subscribe(2L);

        service.closeAllOnShutdown();

        // 이미 닫힌 emitter에는 더 보낼 수 없다.
        for (SseEmitter emitter : new SseEmitter[]{first, second, other}) {
            assertThatThrownBy(() -> emitter.send("ping")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void 종료_뒤에는_보낼_연결이_없어_조용히_넘어간다() {
        service.subscribe(1L);
        service.closeAllOnShutdown();

        assertThatCode(() -> {
            service.sendHeartbeat();
            service.closeAllOnShutdown();
        }).doesNotThrowAnyException();
    }
}
