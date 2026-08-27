package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.request.FormCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormQuestionCreateDTO;
import com.example.kodyjobdam.form.dto.request.FormUpdateDTO;
import com.example.kodyjobdam.form.dto.response.FormResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSummaryResponseDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormQuestionOptionEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;

    private final UserRepository userRepository;

    /** 선생님: 폼 생성 (초안 상태로 저장) */
    @Transactional
    public FormResponseDTO create(FormCreateDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> FormException.notFound("회원이 없습니다."));

        FormEntity form = FormEntity.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(FormStatus.DRAFT)
                .build();

        applyQuestions(form, dto.getQuestions());

        return FormResponseDTO.from(formRepository.save(form));
    }

    /**
     * 선생님: 폼 수정.
     * 이미 제출된 응답과 질문이 어긋나는 것을 막기 위해 초안 상태에서만 허용한다.
     */
    @Transactional
    public FormResponseDTO update(Long formId, FormUpdateDTO dto) {
        FormEntity form = findOrThrow(formId);

        if (!form.isEditable()) {
            throw FormException.badRequest("이미 공개된 폼은 수정할 수 없습니다. 새 폼을 만들어주세요.");
        }

        form.update(dto.getTitle(), dto.getDescription());
        form.clearQuestions();
        applyQuestions(form, dto.getQuestions());

        return FormResponseDTO.from(form);
    }

    /** 선생님: 학생에게 공개 */
    @Transactional
    public FormResponseDTO publish(Long formId) {
        FormEntity form = findOrThrow(formId);

        if (form.getStatus() != FormStatus.DRAFT) {
            throw FormException.badRequest("초안 상태의 폼만 공개할 수 있습니다.");
        }
        if (form.getQuestions().isEmpty()) {
            throw FormException.badRequest("질문이 없는 폼은 공개할 수 없습니다.");
        }

        form.publish();
        return FormResponseDTO.from(form);
    }

    /** 선생님: 응답 마감 */
    @Transactional
    public FormResponseDTO close(Long formId) {
        FormEntity form = findOrThrow(formId);

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw FormException.badRequest("공개된 폼만 마감할 수 있습니다.");
        }

        form.close();
        return FormResponseDTO.from(form);
    }

    /** 선생님 관리용: 초안 포함 전체 목록 */
    @Transactional(readOnly = true)
    public List<FormSummaryResponseDTO> listForTeacher() {
        return formRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(FormSummaryResponseDTO::from)
                .toList();
    }

    /** 선생님 관리용: 폼 단건 (상태 무관) */
    @Transactional(readOnly = true)
    public FormResponseDTO getForTeacher(Long formId) {
        return FormResponseDTO.from(findOrThrow(formId));
    }

    /** 학생용: 공개된 폼 목록 */
    @Transactional(readOnly = true)
    public List<FormSummaryResponseDTO> listPublished() {
        return formRepository.findByStatusOrderByCreatedAtDesc(FormStatus.PUBLISHED).stream()
                .map(FormSummaryResponseDTO::from)
                .toList();
    }

    /** 학생용: 공개된 폼 단건 */
    @Transactional(readOnly = true)
    public FormResponseDTO getPublished(Long formId) {
        FormEntity form = findOrThrow(formId);

        if (form.getStatus() == FormStatus.DRAFT) {
            throw FormException.notFound("공개된 폼이 아닙니다.");
        }

        return FormResponseDTO.from(form);
    }

    /** 요청으로 들어온 질문 목록을 폼에 반영한다 (순서는 배열 순서를 그대로 따른다) */
    private void applyQuestions(FormEntity form, List<FormQuestionCreateDTO> questions) {
        for (int i = 0; i < questions.size(); i++) {
            FormQuestionCreateDTO dto = questions.get(i);
            validateQuestion(dto);

            FormQuestionEntity question = FormQuestionEntity.builder()
                    .orderIndex(i + 1)
                    .type(dto.getType())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .required(dto.isRequired())
                    .build();

            if (dto.getType().hasOptions()) {
                List<String> options = dto.getOptions();
                for (int j = 0; j < options.size(); j++) {
                    question.addOption(FormQuestionOptionEntity.builder()
                            .orderIndex(j + 1)
                            .label(options.get(j))
                            .build());
                }
            }

            form.addQuestion(question);
        }
    }

    /** 질문 유형과 선택지 구성이 맞는지 검사한다 */
    private void validateQuestion(FormQuestionCreateDTO dto) {
        boolean hasOptions = dto.getOptions() != null && !dto.getOptions().isEmpty();

        if (dto.getType().hasOptions() && !hasOptions) {
            throw FormException.badRequest("선택형 질문에는 선택지가 1개 이상 필요합니다: " + dto.getTitle());
        }
        if (!dto.getType().hasOptions() && hasOptions) {
            throw FormException.badRequest("주관식 질문에는 선택지를 넣을 수 없습니다: " + dto.getTitle());
        }
        if (hasOptions && dto.getOptions().stream().anyMatch(label -> label == null || label.isBlank())) {
            throw FormException.badRequest("빈 선택지는 넣을 수 없습니다: " + dto.getTitle());
        }
    }

    private FormEntity findOrThrow(Long formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> FormException.notFound("폼을 찾을 수 없습니다."));
    }
}
