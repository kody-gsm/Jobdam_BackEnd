-- 합격 보고를 면접 합격만 남기고, 학번·이름·기업 이름만 저장하도록 바꾼다.
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.
--
-- 바뀌는 것
-- - 서류 합격(type = 'DOCUMENT') 보고와 그 알림을 지운다.
-- - 받는 선생님(teacher_id)을 없앤다. 이제 보고하면 모든 진로 선생님(TEACHER)에게 알림이 간다.
-- - type, company_phone, interview_date 컬럼을 없앤다.
-- - 보고한 순간의 학번·이름을 student_number, student_name 컬럼에 복사해 둔다.
--   student_id는 학생이 '내가 보낸 보고'를 찾을 때만 쓰므로 남긴다.
-- - created_at을 한국 시간 제출 시각 submitted_at으로 바꾼다.
--
-- 이 스크립트는 한 번만 실행한다(다시 실행하면 3)의 ADD COLUMN에서 실패한다).
--
-- 제약조건·인덱스 이름은 docs/pass-report-migration.sql 기준이다.
-- 실행 전에 SHOW CREATE TABLE pass_report; 로 이름이 같은지 확인한다.
-- 한글이 들어가지 않지만, CLI로 실행한다면 접속 직후 SET NAMES utf8mb4; 를 먼저 실행한다.
-- 오류가 나면 바로 멈추도록 실행한다(mysql --force, Workbench의 '오류가 나도 계속' 옵션을 쓰지 않는다).

-- MySQL Workbench는 기본으로 safe update를 켜 두어, 키 조건이 없는 DELETE/UPDATE를 거부한다(ERROR 1175).
-- 이 접속에서만 끈다.
SET SQL_SAFE_UPDATES = 0;

-- 1) 서류 합격 보고의 알림을 먼저 지운다. (notification.target_id 가 pass_report.id 를 가리킨다)
DELETE n
FROM notification n
JOIN pass_report p ON p.id = n.target_id
WHERE n.type = 'PASS_REPORTED'
  AND p.type = 'DOCUMENT';

-- 2) 서류 합격 보고를 지운다.
DELETE FROM pass_report WHERE type = 'DOCUMENT';

-- 3) 학번·이름 컬럼을 추가하고 남은 면접 합격 보고에 학생 정보를 채운다.
ALTER TABLE pass_report
    ADD COLUMN student_number VARCHAR(20) NULL AFTER student_id,
    ADD COLUMN student_name VARCHAR(50) NULL AFTER student_number;

UPDATE pass_report p
JOIN user u ON u.id = p.student_id
SET p.student_number = u.student_number,
    p.student_name = u.name;

ALTER TABLE pass_report
    MODIFY COLUMN student_number VARCHAR(20) NOT NULL,
    MODIFY COLUMN student_name VARCHAR(50) NOT NULL;

-- 4) 받는 선생님과 쓰지 않는 컬럼을 없앤다. 외래 키를 먼저 지워야 인덱스를 지울 수 있다.
ALTER TABLE pass_report DROP FOREIGN KEY fk_pass_report_teacher;

ALTER TABLE pass_report
    DROP INDEX idx_pass_report_teacher_created,
    DROP COLUMN teacher_id,
    DROP COLUMN type,
    DROP COLUMN company_phone,
    DROP COLUMN interview_date;

-- 5) created_at을 제출 시각 submitted_at으로 이름을 바꾼다.
--    앱이 한국 시간을 직접 넣으므로 DB 기본값은 없앤다.
--    created_at을 쓰던 인덱스(idx_pass_report_student_created)는 이름이 바뀐 컬럼을 그대로 따라간다.
--    시간대 보정은 하지 않는다. 기존 created_at 원본 값은 이미 한국 시간이고,
--    앱 시간대를 Asia/Seoul로 맞추는 배포(docs/app-timezone-seoul-migration.sql)와 함께 나가면 그대로 맞게 읽힌다.
ALTER TABLE pass_report
    CHANGE COLUMN created_at submitted_at DATETIME(6) NOT NULL;

-- 6) 선생님 전체 목록은 학번순, 같은 학생은 먼저 제출한 순으로 읽는다.
CREATE INDEX idx_pass_report_student_number ON pass_report (student_number, submitted_at);
