package com.example.kodyjobdam.form.repository;

import com.example.kodyjobdam.form.entity.FormFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FormFileRepository extends JpaRepository<FormFileEntity, Long> {

    List<FormFileEntity> findByFormId(Long formId);

    /** 올려만 두고 어떤 답변에도 붙지 않은 파일 (제출을 중간에 그만둔 경우) */
    @Query("""
            select f from FormFileEntity f
            where f.uploadedAt < :threshold
              and not exists (select 1 from FormAnswerEntity a where a.file = f)
            """)
    List<FormFileEntity> findOrphans(@Param("threshold") LocalDateTime threshold);
}
