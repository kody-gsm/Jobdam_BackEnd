package com.example.kodyjobdam.course.repository;

import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CourseEntity> findAllByDateAndPeriodAndTeacher_Id(LocalDate date, String period, Long teacherId);

    @EntityGraph(attributePaths = {"user", "teacher"})
    List<CourseEntity> findByUser_id(Long userId);

    @EntityGraph(attributePaths = {"user", "teacher"})
    List<CourseEntity> findByTeacher_Id(Long teacherId);

    @EntityGraph(attributePaths = {"user", "teacher"})
    List<CourseEntity> findByTeacher_IdAndStateOrderByDateAscPeriodAsc(Long teacherId, StateEnum state);
}
