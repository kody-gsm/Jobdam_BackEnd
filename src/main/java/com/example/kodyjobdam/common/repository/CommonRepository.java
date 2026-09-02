package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommonRepository extends JpaRepository<CommonEntity, Long> {

    List<CommonEntity> findAllByDateAndPeriod(LocalDate date, String period);

    // 해당 선생님의 그 시간대 예약 (잠금·중복 확인용)
    List<CommonEntity> findAllByDateAndPeriodAndTeacherId(LocalDate date, String period, Long teacherId);

    List<CommonEntity> findByUser_id(Long userId);

    // 선생님별 상태 목록 (WAITING = 대기, RESERVED = 수락 완료)
    List<CommonEntity> findByTeacherIdAndStateOrderByDateAscPeriodAsc(Long teacherId, StateEnum state);
}