-- DataGSM 졸업생·자퇴생의 학번이 null일 수 있으므로 주간 동기화 배포 전에 실행한다.
ALTER TABLE `user`
    MODIFY COLUMN `student_number` VARCHAR(255) NULL;
