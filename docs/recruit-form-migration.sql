-- 채용 공고에 자동 생성 지원 폼 연결
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 컬럼이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.

ALTER TABLE recruit
    ADD COLUMN form_id BIGINT NULL;

ALTER TABLE recruit
    ADD CONSTRAINT fk_recruit_form FOREIGN KEY (form_id) REFERENCES form (id);

-- 기존 공고에는 지원 폼이 없다. 필요하면 선생님이 폼을 새로 만들어 연결한다.
