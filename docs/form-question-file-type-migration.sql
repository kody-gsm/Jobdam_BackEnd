-- 폼 질문 유형에 파일 첨부(FILE) 추가
-- form-file-migration.sql 에서 이 문장이 누락되어, 채용 공고 분석이 지원 폼을 자동 생성할 때
-- 포트폴리오(FILE) 질문 INSERT가 "Data truncated for column 'type'" 로 실패했다.
--
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이고,
-- Hibernate 6.2+ 는 MySQL에서 @Enumerated(STRING)을 네이티브 enum 컬럼으로 만든다.
-- validate 는 컬럼 존재만 확인하고 enum 값 목록까지는 보지 않으므로 앱은 정상 기동하지만,
-- 새 값을 INSERT 하는 순간 실패한다. 값 추가만 하는 변경이라 기존 데이터는 보존된다.

ALTER TABLE form_question
    MODIFY COLUMN type ENUM('SHORT_TEXT','LONG_TEXT','SINGLE_CHOICE',
                            'MULTIPLE_CHOICE','DROPDOWN','NUMBER','DATE','FILE') DEFAULT NULL;
