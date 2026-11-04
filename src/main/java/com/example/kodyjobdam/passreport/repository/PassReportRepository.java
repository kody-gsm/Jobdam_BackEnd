package com.example.kodyjobdam.passreport.repository;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassReportRepository extends JpaRepository<PassReportEntity, Long> {

    // 학생: 내가 보낸 면접 합격 보고 최신순
    List<PassReportEntity> findByStudent_IdOrderBySubmittedAtDescIdDesc(Long studentId);

    // 선생님: 전체 면접 합격 보고 학번순. 한 학생의 보고는 먼저 보낸 것부터 나온다.
    List<PassReportEntity> findAllByOrderByStudentNumberAscSubmittedAtAscIdAsc();
}
