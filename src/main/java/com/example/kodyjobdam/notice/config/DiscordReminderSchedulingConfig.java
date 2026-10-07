package com.example.kodyjobdam.notice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class DiscordReminderSchedulingConfig {

    /** 기존 예약·정리 작업은 별도 기본 스레드에서 계속 실행한다. */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        return scheduler("scheduling-");
    }

    /** 디스코드 DM의 네트워크 대기가 다른 예약 작업을 지연시키지 않게 한다. */
    @Bean(name = "discordReminderTaskScheduler")
    public ThreadPoolTaskScheduler discordReminderTaskScheduler() {
        return scheduler("discord-reminder-");
    }

    private ThreadPoolTaskScheduler scheduler(String prefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(prefix);
        return scheduler;
    }
}
