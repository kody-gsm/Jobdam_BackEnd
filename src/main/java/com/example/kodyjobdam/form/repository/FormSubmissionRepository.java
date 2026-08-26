package com.example.kodyjobdam.form.repository;

import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, Long> {

    // 중복 제출 방지용
    boolean existsByFormIdAndUserId(Long formId, Long userId);

    // 선생님: 폼별 제출 목록 최신순
    List<FormSubmissionEntity> findByFormIdOrderBySubmittedAtDesc(Long formId);

    // 학생: 내가 제출한 응답
    Optional<FormSubmissionEntity> findByFormIdAndUserId(Long formId, Long userId);
}
