package com.example.kodyjobdam.global.config;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 블루-그린 배포 중에는 두 컨테이너가 함께 떠 있으므로, 새로 만드는 @Scheduled 작업도 잠금을 빠뜨리지 않게 막는다.
 */
class ScheduledJobLockTest {

    /** 컨테이너마다 따로 돌아야 하는 작업. 잠그면 다른 컨테이너의 연결이 heartbeat를 못 받는다. */
    private static final Set<String> PER_INSTANCE_JOBS = Set.of(
            "NotificationSseService#sendHeartbeat"
    );

    @Test
    void 모든_예약_작업에_SchedulerLock이_붙어_있다() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<String> scheduled = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.example.kodyjobdam")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Scheduled.class)) continue;
                String name = type.getSimpleName() + "#" + method.getName();
                scheduled.add(name);
                if (!PER_INSTANCE_JOBS.contains(name) && !method.isAnnotationPresent(SchedulerLock.class)) {
                    missing.add(name);
                }
            }
        }

        assertThat(scheduled).as("스캔이 예약 작업을 찾아야 한다").isNotEmpty();
        assertThat(missing).as("@SchedulerLock이 없는 @Scheduled 작업").isEmpty();
    }
}
