-- 운영 환경은 JPA_DDL_AUTO=validate이므로 배포 전에 실행한다.
CREATE TABLE IF NOT EXISTS statistic_counter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    metric_date DATE NOT NULL,
    category VARCHAR(16) NOT NULL,
    dimension VARCHAR(240) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    call_count BIGINT NOT NULL DEFAULT 0,
    total_duration_ms BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_statistic_counter UNIQUE (metric_date, category, dimension, outcome)
);
