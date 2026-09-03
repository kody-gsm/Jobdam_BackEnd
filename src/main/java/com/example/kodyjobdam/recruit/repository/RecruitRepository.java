package com.example.kodyjobdam.recruit.repository;

import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruitRepository extends JpaRepository<RecruitEntity, Long> {

    // 학생/공개용: 공개된 공고만 최신순
    List<RecruitEntity> findByStatusOrderByCreatedAtDesc(RecruitStatus status);

    // 선생님 관리용: 초안 포함 전체 최신순
    List<RecruitEntity> findAllByOrderByCreatedAtDesc();
}
