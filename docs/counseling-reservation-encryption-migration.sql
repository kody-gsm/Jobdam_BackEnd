-- 상담 신청 정보 암호화 저장을 위한 MySQL 스키마 변경 예시입니다.
-- 적용 전 COUNSELING_RESERVATION_CRYPTO_KEY 환경변수를 먼저 준비하세요.
-- 기존 common/course 데이터는 user_id, title, content에 평문 또는 식별 가능한 값이 남아 있습니다.
-- 운영 반영 시 기존 데이터를 백업 후 삭제하거나, 별도 스크립트로 encrypted_* 컬럼에 backfill한 뒤 원본 컬럼을 제거하세요.

ALTER TABLE common
    ADD COLUMN category VARCHAR(20) NULL,
    ADD COLUMN submitter_hash VARCHAR(64) NULL,
    ADD COLUMN encrypted_user_id TEXT NULL,
    ADD COLUMN encrypted_user_name TEXT NULL,
    ADD COLUMN encrypted_student_number TEXT NULL,
    ADD COLUMN encrypted_title TEXT NULL,
    ADD COLUMN encrypted_content TEXT NULL;

ALTER TABLE course
    ADD COLUMN category VARCHAR(20) NULL,
    ADD COLUMN submitter_hash VARCHAR(64) NULL,
    ADD COLUMN encrypted_user_id TEXT NULL,
    ADD COLUMN encrypted_user_name TEXT NULL,
    ADD COLUMN encrypted_student_number TEXT NULL,
    ADD COLUMN encrypted_title TEXT NULL,
    ADD COLUMN encrypted_content TEXT NULL;

-- 기존 데이터를 정리하거나 암호화 backfill한 뒤에만 실행하세요.
-- ALTER TABLE common
--     DROP FOREIGN KEY <common_user_fk_name>,
--     DROP COLUMN user_id,
--     DROP COLUMN title,
--     DROP COLUMN content;
--
-- ALTER TABLE course
--     DROP FOREIGN KEY <course_user_fk_name>,
--     DROP COLUMN user_id,
--     DROP COLUMN title,
--     DROP COLUMN content;
