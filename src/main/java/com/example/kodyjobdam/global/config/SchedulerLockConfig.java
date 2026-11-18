package com.example.kodyjobdam.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 블루-그린 배포 중에는 옛 컨테이너와 새 컨테이너가 잠깐 함께 떠 있어서 @Scheduled 작업이 두 번 돌 수 있다.
 * 작업마다 @SchedulerLock을 붙이고, 잠금은 두 컨테이너가 같이 보는 DB의 shedlock 테이블에 둔다.
 *
 * 잠금 시각은 DB 시각(usingDbTime)으로 잰다. 컨테이너마다 시계가 조금 달라도 잠금이 어긋나지 않는다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerLockConfig {

    /**
     * 운영은 ddl-auto=validate라 테이블이 자동으로 생기지 않는다. 엔티티가 아니어서 validate 대상도 아니므로
     * 기동할 때 없으면 만든다. 이미 있으면 아무것도 하지 않으므로 옛 버전과 함께 떠 있어도 안전하다.
     */
    static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS shedlock (
                name       VARCHAR(64)  NOT NULL,
                lock_until TIMESTAMP(3) NOT NULL,
                locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                locked_by  VARCHAR(255) NOT NULL,
                PRIMARY KEY (name)
            )
            """;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(CREATE_TABLE_SQL);
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build());
    }
}
