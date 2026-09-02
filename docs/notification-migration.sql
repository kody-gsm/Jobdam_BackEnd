-- 알림 테이블 추가
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_id BIGINT NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES user (id),
    CONSTRAINT uk_notification_receiver_type_target UNIQUE (receiver_id, type, target_id),
    INDEX idx_notification_receiver_read_created (receiver_id, is_read, created_at),
    INDEX idx_notification_expires_at (expires_at)
);

-- 기존 notification 테이블이 이미 만들어진 환경에서는 expires_at을 먼저 nullable로 추가하고 백필 후 NOT NULL로 변경한다.
-- ALTER TABLE notification ADD COLUMN expires_at DATETIME(6) NULL;
-- UPDATE notification SET expires_at = DATE_ADD(created_at, INTERVAL 90 DAY) WHERE expires_at IS NULL;
-- ALTER TABLE notification MODIFY expires_at DATETIME(6) NOT NULL;
-- CREATE INDEX idx_notification_expires_at ON notification (expires_at);

-- 폼 마감일을 사용할 경우 선택형 컬럼으로 추가한다. 기존 폼은 deadline이 NULL이면 알림 생성 시 기본 보관기간을 사용한다.
ALTER TABLE form ADD COLUMN deadline DATETIME(6) NULL;

-- 일반상담: 기존 allow_id 컬럼을 teacher_id로 안전하게 전환
ALTER TABLE common ADD COLUMN teacher_id BIGINT NULL;
UPDATE common SET teacher_id = allow_id WHERE teacher_id IS NULL AND allow_id IS NOT NULL;

-- 진로상담: 기존 Long teacher_id 컬럼이 있다면 FK만 추가하기 전에 데이터 정합성을 먼저 확인한다.
-- SELECT c.reservation_id, c.teacher_id
-- FROM common c
-- LEFT JOIN user u ON c.teacher_id = u.id AND u.role = 'TEACHER'
-- WHERE c.teacher_id IS NULL OR u.id IS NULL;
--
-- SELECT c.reservation_id, c.teacher_id
-- FROM course c
-- LEFT JOIN user u ON c.teacher_id = u.id AND u.role = 'TEACHER'
-- WHERE c.teacher_id IS NULL OR u.id IS NULL;

-- 위 조회 결과가 없도록 백필 또는 정리한 뒤 NOT NULL과 FK를 적용한다.
ALTER TABLE common MODIFY teacher_id BIGINT NOT NULL;
ALTER TABLE common
    ADD CONSTRAINT fk_common_teacher FOREIGN KEY (teacher_id) REFERENCES user (id);

ALTER TABLE course MODIFY teacher_id BIGINT NOT NULL;
ALTER TABLE course
    ADD CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES user (id);

-- 기존 allow_id를 더 이상 사용하지 않는 것이 확인된 뒤에만 제거한다.
ALTER TABLE common DROP COLUMN allow_id;
