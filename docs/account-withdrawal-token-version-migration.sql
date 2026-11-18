-- 운영 환경은 Hibernate validate를 사용하므로 회원 탈퇴 기능 배포 전에 실행한다.
ALTER TABLE `user`
    ADD COLUMN `auth_token_version` BIGINT NOT NULL DEFAULT 0;
