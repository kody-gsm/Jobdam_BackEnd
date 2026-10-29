package com.example.kodyjobdam.form.repository;

import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, Long> {

    // 중복 제출 방지용
    boolean existsByFormIdAndUserId(Long formId, Long userId);

    // 폼 삭제·질문 교체 가능 여부 판단용
    boolean existsByFormId(Long formId);

    // 선생님: 폼별 제출 목록 최신순
    List<FormSubmissionEntity> findByFormIdOrderBySubmittedAtDesc(Long formId);

    // 선생님: 응답 엑셀·첨부 파일 내려받기용. 학생과 답변·첨부 파일까지 한 번에 읽는다.
    @EntityGraph(attributePaths = {"user", "answers", "answers.file"})
    List<FormSubmissionEntity> findAllWithAnswersByFormId(Long formId);

    // 학생: 내가 제출한 응답
    Optional<FormSubmissionEntity> findByFormIdAndUserId(Long formId, Long userId);

    // 학생 목록의 제출 여부를 한 번에 계산
    @Query("select s.form.id from FormSubmissionEntity s where s.user.id = :userId and s.form.id in :formIds")
    List<Long> findSubmittedFormIds(@Param("userId") Long userId, @Param("formIds") List<Long> formIds);
}
