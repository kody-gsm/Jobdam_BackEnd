-- 기존 counseling_reminder 테이블을 사용하는 운영 DB에 배포 전 적용
ALTER TABLE counseling_reminder
    ADD COLUMN discord_user_id VARCHAR(32) NULL,
    ADD COLUMN discord_message_id VARCHAR(32) NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL;
