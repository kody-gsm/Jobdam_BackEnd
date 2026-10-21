package com.example.kodyjobdam.passreport.repository;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassReportRepository extends JpaRepository<PassReportEntity, Long> {

    // 학생: 내가 보낸 합격 보고 최신순
    @EntityGraph(attributePaths = {"student", "teacher"})
    List<PassReportEntity> findByStudent_IdOrderByCreatedAtDescIdDesc(Long studentId);

    // 선생님: 내가 받은 합격 보고 최신순
    @EntityGraph(attributePaths = {"student", "teacher"})
    List<PassReportEntity> findByTeacher_IdOrderByCreatedAtDescIdDesc(Long teacherId);
}
