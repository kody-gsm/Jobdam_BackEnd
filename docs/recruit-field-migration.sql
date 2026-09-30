-- 채용 공고 직무 분야 컬럼 도입
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이므로 컬럼이 자동 생성되지 않는다.
-- 배포 전에 이 스크립트를 먼저 실행해야 앱이 기동된다.
--
-- 한 공고가 여러 직무를 함께 뽑을 수 있어 쉼표로 이어 한 컬럼에 담는다.
-- 예) "FRONTEND,BACKEND"
-- 값은 RecruitField enum 이름(FRONTEND, BACKEND, FULLSTACK, MOBILE, IOT, AI, SECURITY, ETC).
-- 분야가 없으면 NULL이다.

ALTER TABLE recruit
    ADD COLUMN fields VARCHAR(100) NULL;

-- 길이 100의 근거: 8개 값을 모두 담아도 53자다. 분야를 몇 개 더 늘릴 여유를 둔 값이다.
-- RecruitFieldConverterTest가 이 한계를 검증한다.

-- CHECK 제약은 일부러 걸지 않는다.
-- 애플리케이션이 유일한 기록 주체이고 RecruitFieldConverter가 값을 이미 보증한다. 제약을 걸면
-- 직무를 하나 늘릴 때마다 ALTER TABLE이 필요하고, ddl-auto=update는 기존 CHECK 제약을
-- 절대 갱신하지 않으므로 validate를 통과한 뒤 INSERT 시점에 터진다.

-- 기존 공고에는 직무 분야가 없다. NULL로 두고, 선생님이 공고를 수정할 때 채운다.
-- 응답에는 빈 배열로 나가고 화면에서는 "미정"으로 보여주면 된다.

-- 확인: 공고별 직무 분야
-- SELECT id, company_name, fields FROM recruit ORDER BY id;

-- 특정 분야를 뽑는 공고만 찾을 때는 FIND_IN_SET을 쓴다. (인덱스를 타지 않는다)
-- SELECT id, company_name FROM recruit WHERE FIND_IN_SET('AI', fields);
