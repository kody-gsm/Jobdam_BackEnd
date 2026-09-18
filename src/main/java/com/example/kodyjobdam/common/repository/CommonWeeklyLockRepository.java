package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonWeeklyLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommonWeeklyLockRepository extends JpaRepository<CommonWeeklyLockEntity, Long> {

    boolean existsByTeacher_IdAndDayOfWeekAndPeriod(Long teacherId, DayOfWeek dayOfWeek, String period);

    Optional<CommonWeeklyLockEntity> findByTeacher_IdAndDayOfWeekAndPeriod(
            Long teacherId, DayOfWeek dayOfWeek, String period);

    List<CommonWeeklyLockEntity> findAllByTeacher_Id(Long teacherId);
}
