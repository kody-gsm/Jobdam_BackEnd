package com.example.kodyjobdam.course.repository;

import com.example.kodyjobdam.course.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CourseEntity> findByUser_id(Long userId);

    List<CourseEntity> findByUser_idAndDateGreaterThanEqual(Long userId, LocalDate date);
}
