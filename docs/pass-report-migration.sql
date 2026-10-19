-- 학생이 서류·면접 합격을 진로 선생님에게 알리는 합격 보고 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 테이블이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.
--
-- type 값은 PassType enum 이름(DOCUMENT: 서류 합격, INTERVIEW: 면접 합격).
-- interview_date는 서류 합격일 때만 있고, 면접 합격이면 NULL이다.
-- 알림 종류 PASS_REPORTED가 새로 생기지만 notification.type은 VARCHAR(50)이라 바꿀 것이 없다.

CREATE TABLE IF NOT EXISTS pass_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    company_phone VARCHAR(20) NOT NULL,
    interview_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pass_report_student FOREIGN KEY (student_id) REFERENCES user (id),
    CONSTRAINT fk_pass_report_teacher FOREIGN KEY (teacher_id) REFERENCES user (id),
    INDEX idx_pass_report_student_created (student_id, created_at),
    INDEX idx_pass_report_teacher_created (teacher_id, created_at)
);
