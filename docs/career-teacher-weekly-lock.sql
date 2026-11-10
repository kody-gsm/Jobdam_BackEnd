-- 진로 선생님(임경원) 정규 시간표를 매주 반복 잠금으로 등록
--
-- 선생님이 준 시간표에서 상담을 받을 수 없는 칸(23칸)을 course_weekly_lock 에 넣는다.
--   월: 1, 2, 3, 4, 7교시
--   화: 1, 2, 4, 6, 7교시
--   수: 1, 4, 5, 6, 7교시
--   목: 3, 4, 5, 6, 7교시
--   금: 1, 4, 7교시
--
-- 시간표가 바뀐 날(보강, 교체 수업)은 CareerClassLockScheduler 가 나이스 시간표를 읽어 날짜별 LOCKED 행으로 따로 잠근다.
-- 선생님은 /teacher/course/unlock/weekly 로 요일·교시 단위로 풀 수 있다.
-- 반복 잠금은 날짜별 행이 없어서, 특정 날짜 하나만 푸는 것은 지원하지 않는다.
--
-- 이 SQL은 앱의 반복 잠금 API와 달리 그 요일·교시에 잡힌 대기·수락 신청을 취소하지 않는다.
-- 실행 전에 1)에서 겹치는 신청이 없는지 확인한다.
-- (teacher_id, day_of_week, period) 유니크 키가 있어 INSERT IGNORE 로 이미 있는 칸은 건너뛴다. 다시 실행해도 된다.

-- 0) 대상 선생님 확인 (정확히 1명이어야 한다)
SELECT id, name, role FROM user WHERE role = 'TEACHER' AND name = '임경원';

-- 1) 오늘 이후 잠글 요일·교시에 걸린 대기·수락 신청 확인 (0건이어야 한다)
SELECT c.date, DAYNAME(c.date) AS dow, c.period, c.state
FROM course c
JOIN user u ON u.id = c.teacher_id
WHERE u.role = 'TEACHER' AND u.name = '임경원'
  AND c.state IN ('WAITING', 'RESERVED')
  AND c.date >= CURDATE()
  AND (DAYOFWEEK(c.date), c.period) IN (
        (2, '1교시'), (2, '2교시'), (2, '3교시'), (2, '4교시'), (2, '7교시'),
        (3, '1교시'), (3, '2교시'), (3, '4교시'), (3, '6교시'), (3, '7교시'),
        (4, '1교시'), (4, '4교시'), (4, '5교시'), (4, '6교시'), (4, '7교시'),
        (5, '3교시'), (5, '4교시'), (5, '5교시'), (5, '6교시'), (5, '7교시'),
        (6, '1교시'), (6, '4교시'), (6, '7교시'));

-- 2) 반복 잠금 등록
INSERT IGNORE INTO course_weekly_lock (teacher_id, day_of_week, period, created_at)
SELECT u.id, v.day_of_week, v.period, NOW()
FROM user u
JOIN (
          SELECT 'MONDAY' AS day_of_week, '1교시' AS period
    UNION ALL SELECT 'MONDAY', '2교시'
    UNION ALL SELECT 'MONDAY', '3교시'
    UNION ALL SELECT 'MONDAY', '4교시'
    UNION ALL SELECT 'MONDAY', '7교시'
    UNION ALL SELECT 'TUESDAY', '1교시'
    UNION ALL SELECT 'TUESDAY', '2교시'
    UNION ALL SELECT 'TUESDAY', '4교시'
    UNION ALL SELECT 'TUESDAY', '6교시'
    UNION ALL SELECT 'TUESDAY', '7교시'
    UNION ALL SELECT 'WEDNESDAY', '1교시'
    UNION ALL SELECT 'WEDNESDAY', '4교시'
    UNION ALL SELECT 'WEDNESDAY', '5교시'
    UNION ALL SELECT 'WEDNESDAY', '6교시'
    UNION ALL SELECT 'WEDNESDAY', '7교시'
    UNION ALL SELECT 'THURSDAY', '3교시'
    UNION ALL SELECT 'THURSDAY', '4교시'
    UNION ALL SELECT 'THURSDAY', '5교시'
    UNION ALL SELECT 'THURSDAY', '6교시'
    UNION ALL SELECT 'THURSDAY', '7교시'
    UNION ALL SELECT 'FRIDAY', '1교시'
    UNION ALL SELECT 'FRIDAY', '4교시'
    UNION ALL SELECT 'FRIDAY', '7교시'
) v
WHERE u.role = 'TEACHER' AND u.name = '임경원';

-- 3) 결과 확인 (23행)
SELECT w.day_of_week, GROUP_CONCAT(w.period ORDER BY w.period SEPARATOR ', ') AS periods, COUNT(*) AS cnt
FROM course_weekly_lock w
JOIN user u ON u.id = w.teacher_id
WHERE u.role = 'TEACHER' AND u.name = '임경원'
GROUP BY w.day_of_week
ORDER BY FIELD(w.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY');
