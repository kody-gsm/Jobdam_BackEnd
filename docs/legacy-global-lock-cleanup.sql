-- 선생님 구분 전(2026-09-02 이전)에 만든 전체 잠금 행 정리
-- 당시 잠금은 선생님 없이 저장돼 teacher_id가 비어 있고, 누가 잠갔는지 알 수 없다.
-- 애플리케이션은 teacher_id로만 잠금을 찾으므로 이 행들은 어디에도 쓰이지 않는다.
-- 실행 전에 백업하고, 먼저 SELECT로 지울 행을 확인한다.

-- 1) 지울 행 확인
SELECT reservation_id, date, period, state FROM common WHERE state = 'LOCKED' AND teacher_id IS NULL;
SELECT reservation_id, date, period, state FROM course WHERE state = 'LOCKED' AND teacher_id IS NULL;

-- 2) 삭제
DELETE FROM common WHERE state = 'LOCKED' AND teacher_id IS NULL;
DELETE FROM course WHERE state = 'LOCKED' AND teacher_id IS NULL;
