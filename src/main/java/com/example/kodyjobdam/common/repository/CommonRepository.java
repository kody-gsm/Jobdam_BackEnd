package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CommonRepository extends JpaRepository<CommonEntity, Long> {

    List<CommonEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CommonEntity> findAllByDateAndPeriodAndTeacher_Id(LocalDate date, String period, Long teacherId);

    List<CommonEntity> findAllByDateAndTeacher_IdOrderByPeriodAsc(LocalDate date, Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findBySubmitterHash(String submitterHash);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findByTeacher_Id(Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findByTeacher_IdAndStateOrderByDateAscPeriodAsc(Long teacherId, StateEnum state);
}
