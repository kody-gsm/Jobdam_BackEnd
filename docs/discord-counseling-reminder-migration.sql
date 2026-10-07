-- 운영 환경(application-prod.properties의 ddl-auto=validate) 배포 전 적용
ALTER TABLE `user` ADD COLUMN discord_user_id VARCHAR(32) NULL;
ALTER TABLE `user` ADD CONSTRAINT uk_user_discord_user_id UNIQUE (discord_user_id);

CREATE TABLE counseling_reminder (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reservation_kind VARCHAR(16) NOT NULL,
    reservation_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    hours_before INT NOT NULL,
    sent_at DATETIME(6) NULL,
    claimed_at DATETIME(6) NULL,
    claim_token VARCHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_reminder_slot UNIQUE
        (reservation_kind, reservation_id, starts_at, hours_before)
);
