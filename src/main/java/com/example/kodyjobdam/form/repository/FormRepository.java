package com.example.kodyjobdam.form.repository;

import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<FormEntity, Long> {

    // 선생님 관리용: 초안 포함 전체 최신순
    List<FormEntity> findAllByOrderByCreatedAtDesc();

    List<FormEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 학생용: 공개된 폼만 최신순
    List<FormEntity> findByStatusOrderByCreatedAtDesc(FormStatus status);
}
