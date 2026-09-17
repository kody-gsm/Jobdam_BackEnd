-- 선생님이 요일·교시 단위로 매주 반복해서 잠글 수 있는 기능 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 테이블이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.

CREATE TABLE IF NOT EXISTS common_weekly_lock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    period VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_common_weekly_lock_teacher FOREIGN KEY (teacher_id) REFERENCES user (id),
    CONSTRAINT uk_common_weekly_lock_teacher_day_period UNIQUE (teacher_id, day_of_week, period)
);

CREATE TABLE IF NOT EXISTS course_weekly_lock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    period VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_course_weekly_lock_teacher FOREIGN KEY (teacher_id) REFERENCES user (id),
    CONSTRAINT uk_course_weekly_lock_teacher_day_period UNIQUE (teacher_id, day_of_week, period)
);
