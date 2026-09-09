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
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormService {

    /** 채용 공고에 자동으로 붙는 지원 폼 제목 접미사 */
    private static final String RECRUIT_FORM_TITLE_SUFFIX = " 지원서";

    private final FormRepository formRepository;

    private final FormSubmissionRepository submissionRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final NotificationExpirationService notificationExpirationService;

    /**
     * 채용 공고에 딸린 기본 지원 폼을 만든다.
     * 기본 질문은 학번·이름·포트폴리오이며, 선생님이 초안 상태에서 자유롭게 고칠 수 있다.
     */
    @Transactional
    public FormEntity createForRecruit(User teacher, String companyName, LocalDateTime deadline) {
        String company = (companyName == null || companyName.isBlank()) ? "채용" : companyName.trim();

        FormEntity form = FormEntity.builder()
                .user(teacher)
                .title(company + RECRUIT_FORM_TITLE_SUFFIX)
                .description(company + " 지원을 원하는 학생은 아래 항목을 작성해주세요.")
                .deadline(deadline)
                .status(FormStatus.DRAFT)
                .build();

        form.addQuestion(shortTextQuestion(1, "학번", "예) 1101", true));
        form.addQuestion(shortTextQuestion(2, "이름", null, true));
        form.addQuestion(shortTextQuestion(3, "포트폴리오", "포트폴리오 링크를 입력해주세요.", false));

        return formRepository.save(form);
    }

    /**
     * 채용 공고 공개에 맞춰 딸린 지원 폼도 함께 공개한다.
     * 알림은 공고 쪽에서 한 번만 보내므로 여기서는 상태만 바꾼다.
     */
    @Transactional
    public void publishForRecruit(Long formId) {
        formRepository.findById(formId)
                .filter(form -> form.getStatus() == FormStatus.DRAFT)
                .ifPresent(FormEntity::publish);
    }

    /**
     * 채용 공고 삭제에 맞춰 딸린 지원 폼도 정리한다.
     * 이미 제출된 응답이 있으면 지원 기록이 사라지지 않도록 폼을 남겨둔다.
     */
    @Transactional
    public void deleteForRecruit(Long formId) {
        formRepository.findById(formId)
                .filter(form -> !submissionRepository.existsByFormId(formId))
                .ifPresent(formRepository::delete);
    }

    /** 선생님: 폼 생성 (초안 상태로 저장) */
    @Transactional
    public FormResponseDTO create(FormCreateDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> FormException.notFound("회원이 없습니다."));

        FormEntity form = FormEntity.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .deadline(dto.getDeadline())
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
    public FormResponseDTO update(Long formId, FormUpdateDTO dto, Long teacherId) {
        FormEntity form = findOrThrow(formId);
        validateOwner(form, teacherId);

        form.update(dto.getTitle(), dto.getDescription(), dto.getDeadline());

        if (dto.getQuestions() != null) {
            if (dto.getQuestions().isEmpty()) {
                throw FormException.badRequest("질문을 1개 이상 추가해주세요.");
            }
            // 답변이 질문을 참조하므로 응답이 들어온 뒤에는 질문을 갈아끼울 수 없다.
            if (submissionRepository.existsByFormId(formId)) {
                throw FormException.conflict("이미 응답이 제출된 폼은 질문을 바꿀 수 없습니다.");
            }

            form.clearQuestions();
            applyQuestions(form, dto.getQuestions());
        }

        return FormResponseDTO.from(form);
    }

    /** 선생님: 폼 삭제 (응답이 하나라도 있으면 지울 수 없다) */
    @Transactional
    public void delete(Long formId, Long teacherId) {
        FormEntity form = findOrThrow(formId);
        validateOwner(form, teacherId);

        if (submissionRepository.existsByFormId(formId)) {
            throw FormException.conflict("응답이 제출된 폼은 삭제할 수 없습니다.");
        }

        formRepository.delete(form);
    }

    /** 선생님: 학생에게 공개 */
    @Transactional
    public FormResponseDTO publish(Long formId, Long teacherId) {
        FormEntity form = findOrThrow(formId);
        validateOwner(form, teacherId);

        if (form.getStatus() != FormStatus.DRAFT) {
            throw FormException.badRequest("초안 상태의 폼만 공개할 수 있습니다.");
        }
        if (form.getQuestions().isEmpty()) {
            throw FormException.badRequest("질문이 없는 폼은 공개할 수 없습니다.");
        }

        form.publish();
        notificationService.notifyAllStudents(
                NotificationType.FORM_PUBLISHED,
                "새로운 폼",
                form.getTitle() + " 폼이 게시되었습니다.",
                form.getId(),
                "/form/" + form.getId(),
                notificationExpirationService.formExpiresAt(form.getDeadline())
        );
        return FormResponseDTO.from(form);
    }

    /** 선생님: 응답 마감 */
    @Transactional
    public FormResponseDTO close(Long formId, Long teacherId) {
        FormEntity form = findOrThrow(formId);
        validateOwner(form, teacherId);

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw FormException.badRequest("공개된 폼만 마감할 수 있습니다.");
        }

        form.close();
        return FormResponseDTO.from(form);
    }

    /** 선생님 관리용: 초안 포함 전체 목록 */
    @Transactional(readOnly = true)
    public List<FormSummaryResponseDTO> listForTeacher(Long teacherId) {
        return formRepository.findByUserIdOrderByCreatedAtDesc(teacherId).stream()
                .map(FormSummaryResponseDTO::from)
                .toList();
    }

    /** 선생님 관리용: 폼 단건 (상태 무관) */
    @Transactional(readOnly = true)
    public FormResponseDTO getForTeacher(Long formId, Long teacherId) {
        FormEntity form = findOrThrow(formId);
        validateOwner(form, teacherId);
        return FormResponseDTO.from(form);
    }

    /** 학생용: 공개된 폼 목록 */
    @Transactional(readOnly = true)
    public List<FormSummaryResponseDTO> listPublished() {
        return formRepository.findByStatusOrderByCreatedAtDesc(FormStatus.PUBLISHED).stream()
                .map(FormSummaryResponseDTO::from)
                .toList();
    }

    /** 학생용: 공개된 폼 단건 (초안·마감된 폼은 보이지 않는다) */
    @Transactional(readOnly = true)
    public FormResponseDTO getPublished(Long formId) {
        FormEntity form = findOrThrow(formId);

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw FormException.notFound("공개된 폼이 아닙니다.");
        }

        return FormResponseDTO.from(form);
    }

    /** 기본 지원 폼에 쓰는 단답형 질문 */
    private FormQuestionEntity shortTextQuestion(int orderIndex, String title, String description, boolean required) {
        return FormQuestionEntity.builder()
                .orderIndex(orderIndex)
                .type(QuestionType.SHORT_TEXT)
                .title(title)
                .description(description)
                .required(required)
                .build();
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

    private void validateOwner(FormEntity form, Long teacherId) {
        if (form.getUser() == null || !form.getUser().getId().equals(teacherId)) {
            throw FormException.forbidden("폼을 관리할 권한이 없습니다.");
        }
    }
}
