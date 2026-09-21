package com.example.kodyjobdam.course.repository;

import com.example.kodyjobdam.course.entity.CourseWeeklyLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseWeeklyLockRepository extends JpaRepository<CourseWeeklyLockEntity, Long> {

    boolean existsByTeacher_IdAndDayOfWeekAndPeriod(Long teacherId, DayOfWeek dayOfWeek, String period);

    Optional<CourseWeeklyLockEntity> findByTeacher_IdAndDayOfWeekAndPeriod(
            Long teacherId, DayOfWeek dayOfWeek, String period);

    List<CourseWeeklyLockEntity> findAllByTeacher_Id(Long teacherId);
}
