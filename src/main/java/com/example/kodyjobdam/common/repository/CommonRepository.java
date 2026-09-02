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

    List<CommonEntity> findByUser_id(Long userId);

    // 선생님이 수락한 예약
    List<CommonEntity> findByAllowId(Long allowId);

    // 선생님이 검토해야 할 대기중인 예약
    List<CommonEntity> findByStateOrderByDateAscPeriodAsc(StateEnum state);
}