-- 연결된 지원 폼 마감 시 공고도 CLOSED로 전환할 수 있도록 상태 값 확장.
-- 운영(prod)은 ddl-auto=validate이므로 서버 배포 전에 실행한다.
ALTER TABLE recruit
    MODIFY COLUMN status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NULL;
