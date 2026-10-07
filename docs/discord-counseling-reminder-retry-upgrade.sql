-- 기존 discord-counseling-reminder-migration.sql을 적용한 DB에서 한 번 실행
ALTER TABLE counseling_reminder
    MODIFY COLUMN sent_at DATETIME(6) NULL,
    ADD COLUMN claimed_at DATETIME(6) NULL,
    ADD COLUMN claim_token VARCHAR(36) NULL;
