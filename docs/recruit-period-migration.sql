-- 채용 공고 전형 기간 컬럼 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 컬럼이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.

ALTER TABLE recruit
    ADD COLUMN document_start_date DATE NULL,
    ADD COLUMN document_end_date DATE NULL,
    ADD COLUMN written_exam_start_date DATE NULL,
    ADD COLUMN written_exam_end_date DATE NULL,
    ADD COLUMN practical_exam_start_date DATE NULL,
    ADD COLUMN practical_exam_end_date DATE NULL,
    ADD COLUMN coding_test_start_date DATE NULL,
    ADD COLUMN coding_test_end_date DATE NULL,
    ADD COLUMN interview_start_date DATE NULL,
    ADD COLUMN interview_end_date DATE NULL;

-- 더 이상 사용하지 않는 컬럼. 기존 데이터는 이관하지 않는다.
ALTER TABLE recruit
    DROP COLUMN deadline,
    DROP COLUMN interview_date;
