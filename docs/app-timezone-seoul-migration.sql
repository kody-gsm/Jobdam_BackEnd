-- 운영 앱 시간대를 UTC에서 Asia/Seoul로 바꿀 때 맞춰야 하는 시각 보정
--
-- 왜 필요한가
--   운영 앱 JVM은 UTC였고 DB_URL에는 serverTimezone=Asia/Seoul이 있다.
--   그래서 JDBC 드라이버가 LocalDateTime을 저장할 때 +9시간, 읽을 때 -9시간을 했다.
--   - @CreationTimestamp·@UpdateTimestamp·LocalDateTime.now()로 찍힌 값: DB 원본은 이미 한국 시간이다.
--     → 앱 시간대를 Seoul로 바꾸면 보정 없이 바로 맞게 읽힌다. 손대지 않는다.
--   - 한국 시간 Clock이나 사용자 입력으로 넣은 값: DB 원본이 한국 시간 + 9시간이다.
--     → 앱 시간대를 Seoul로 바꾸면 9시간 늦게 읽히므로 아래에서 9시간 당긴다.
--
-- 언제 실행하나
--   develop을 main에 머지하기(=배포) 직전에 1)을 실행한다.
--   - 모두 0건이면 2)는 실행하지 않는다. (2026-11-04 운영 조회 기준 모두 0건)
--   - 0건이 아니면 2)를 실행하고 곧바로 머지한다.
--     그 사이 옛 앱(UTC)이 새로 쓴 행은 보정되지 않으므로 사이를 짧게 둔다.
--   반드시 한 번만 실행한다. 다시 실행하면 9시간이 또 빠진다.
--
-- pass_report.submitted_at은 넣지 않았다. 합격 보고 변경(docs/pass-report-interview-only-migration.sql)과
-- 이 변경이 같은 배포로 나가므로, 옛 앱(UTC)이 한국 시간 Clock으로 submitted_at을 쓴 행이 생기지 않는다.
--
-- CLI로 실행한다면 접속 직후 SET NAMES utf8mb4; 를 먼저 실행한다.
-- 오류가 나면 바로 멈추도록 실행한다(mysql --force, Workbench의 '오류가 나도 계속' 옵션을 쓰지 않는다).

-- MySQL Workbench의 safe update 모드(ERROR 1175)를 이 접속에서만 끈다.
SET SQL_SAFE_UPDATES = 0;

-- 1) 보정 대상 건수 확인
SELECT 'form.deadline' AS col, COUNT(*) AS cnt FROM form WHERE deadline IS NOT NULL
UNION ALL SELECT 'notification.expires_at', COUNT(*) FROM notification
UNION ALL SELECT 'counseling_reminder', COUNT(*) FROM counseling_reminder;

-- 2) 9시간 당기기 (1)이 모두 0건이면 실행하지 않는다)
START TRANSACTION;

UPDATE form
SET deadline = deadline - INTERVAL 9 HOUR
WHERE deadline IS NOT NULL;

UPDATE notification
SET expires_at = expires_at - INTERVAL 9 HOUR;

-- starts_at은 유니크 키(reservation_kind, reservation_id, starts_at, hours_before)에 들어 있다.
-- 이른 시각부터 옮겨야 옮기는 도중에 다른 행과 겹치지 않는다.
UPDATE counseling_reminder
SET starts_at  = starts_at - INTERVAL 9 HOUR,
    sent_at    = sent_at - INTERVAL 9 HOUR,
    claimed_at = claimed_at - INTERVAL 9 HOUR
ORDER BY starts_at ASC;

COMMIT;
