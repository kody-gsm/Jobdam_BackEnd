-- 선생님 역할 분리: 일반(common) 상담을 담당하는 WEE_TEACHER 추가
-- 진로(course) 상담은 기존 TEACHER가 그대로 담당한다.
--
-- 운영(prod)은 spring.jpa.hibernate.ddl-auto=validate 이고,
-- Hibernate 6.2+ 는 MySQL에서 @Enumerated(STRING)을 네이티브 enum 컬럼으로 만든다.
-- enum 값을 먼저 추가하지 않으면 새 역할 저장이 "Data truncated for column 'role'" 로 실패한다.
-- 배포 전에 이 스크립트를 먼저 실행해야 한다.

-- 1) 역할 enum에 WEE_TEACHER 추가 (이미 적용된 환경에서도 결과가 같다)
ALTER TABLE user
    MODIFY COLUMN role ENUM('STUDENT','TEACHER','ADMIN','WEE_TEACHER') DEFAULT NULL;

-- 2) 일반 상담을 담당할 계정을 WEE_TEACHER로 바꾼다.
--    대상 계정은 운영에서 직접 확인해 채운다.
--    바꾸지 않으면 GET /student/common/teachers 가 빈 목록을 돌려주고 일반 상담을 신청할 수 없다.
-- UPDATE user SET role = 'WEE_TEACHER' WHERE email IN ('...@gsm.hs.kr');
