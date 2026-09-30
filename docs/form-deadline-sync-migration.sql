-- 지원 폼 마감일을 공고 서류 접수 종료일에 맞춘다
-- 스키마 변경은 없다. 이미 갈라져 저장된 값만 바로잡는 데이터 정리 스크립트다.
--
-- 왜 갈라졌나
--   폼 마감일을 LocalTime.MAX(23:59:59.999999999)로 저장했다. MySQL DATETIME은 넘치는
--   소수점 초를 버리지 않고 반올림하므로 2026-09-29 23:59:59.999999999 가
--   2026-09-30 00:00:00 으로 올라간다. 공고는 종료일(DATE)을 그대로 두므로 하루 어긋난다.
--   폼을 폼 화면에서 직접 수정해 마감일만 바꾼 경우도 같은 결과가 된다.
--
-- 코드는 이제 23:59:59(소수점 없음)로 저장하고, 공고에 딸린 폼의 마감일은
-- 공고의 서류 접수 종료일에서만 나온다. 아래 UPDATE로 기존 행을 같은 기준에 맞춘다.

-- 1) 바로잡기 전 확인: 공고 종료일과 폼 마감일이 어긋난 행
SELECT r.id            AS recruit_id,
       r.company_name,
       r.document_end_date,
       f.id            AS form_id,
       f.deadline      AS form_deadline
FROM recruit r
         JOIN form f ON f.id = r.form_id
WHERE r.document_end_date IS NOT NULL
  AND (f.deadline IS NULL
    OR f.deadline <> TIMESTAMP(r.document_end_date, '23:59:59'));

-- 2) 공고에 딸린 폼의 마감일을 종료일 23:59:59로 맞춘다
UPDATE form f
    JOIN recruit r ON r.form_id = f.id
SET f.deadline = TIMESTAMP(r.document_end_date, '23:59:59')
WHERE r.document_end_date IS NOT NULL
  AND (f.deadline IS NULL
    OR f.deadline <> TIMESTAMP(r.document_end_date, '23:59:59'));

-- 3) 공고와 무관한 폼 중 소수점 초 반올림으로 자정에 걸린 행
--    폼 단독 생성 경로는 LocalTime.MAX를 쓰지 않으므로 보통 0건이다. 있으면 전날 23:59:59로 되돌린다.
UPDATE form f
SET f.deadline = TIMESTAMP(DATE_SUB(DATE(f.deadline), INTERVAL 1 DAY), '23:59:59')
WHERE f.id NOT IN (SELECT form_id FROM recruit WHERE form_id IS NOT NULL)
  AND f.deadline IS NOT NULL
  AND TIME(f.deadline) = '00:00:00';

-- 4) 바로잡은 뒤 확인: 결과가 0건이어야 한다
SELECT r.id AS recruit_id, r.document_end_date, f.id AS form_id, f.deadline
FROM recruit r
         JOIN form f ON f.id = r.form_id
WHERE r.document_end_date IS NOT NULL
  AND f.deadline <> TIMESTAMP(r.document_end_date, '23:59:59');

-- 5) 사람이 판단할 행: 공고 접수 기간이 "미정"인데 폼에만 마감일이 있는 경우.
--    새 규칙에서는 공고 종료일이 단일 기준이지만, 마감일을 지우면 제출이 무제한으로 열린다.
--    공고의 서류 접수 기간을 채워 넣을지, 폼 마감일을 지울지 확인한 뒤 손으로 처리한다.
SELECT r.id AS recruit_id, r.company_name, f.id AS form_id, f.deadline AS form_deadline
FROM recruit r
         JOIN form f ON f.id = r.form_id
WHERE r.document_end_date IS NULL
  AND f.deadline IS NOT NULL;
