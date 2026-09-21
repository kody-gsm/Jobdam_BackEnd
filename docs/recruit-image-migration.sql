-- 채용 공고 이미지 컬럼 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 컬럼이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.

ALTER TABLE recruit
    ADD COLUMN image_url VARCHAR(255) NULL;
