-- 폼 답변 파일 첨부(포트폴리오) 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 테이블/컬럼이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.

CREATE TABLE form_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    form_id       BIGINT       NULL,
    user_id       BIGINT       NULL,
    original_name VARCHAR(255) NULL,
    stored_name   VARCHAR(255) NULL,
    content_type  VARCHAR(255) NULL,
    size          BIGINT       NOT NULL,
    uploaded_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_file_stored_name (stored_name),
    CONSTRAINT fk_form_file_form FOREIGN KEY (form_id) REFERENCES form (id),
    CONSTRAINT fk_form_file_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE = InnoDB;

ALTER TABLE form_answer
    ADD COLUMN file_id BIGINT NULL;

ALTER TABLE form_answer
    ADD CONSTRAINT fk_form_answer_file FOREIGN KEY (file_id) REFERENCES form_file (id);
