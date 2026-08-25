package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.form.dto.request.FormAnswerDTO;
import com.example.kodyjobdam.form.dto.request.FormSubmitDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionResponseDTO;
import com.example.kodyjobdam.form.dto.response.FormSubmissionSummaryResponseDTO;
import com.example.kodyjobdam.form.entity.*;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormSubmissionService {

    private final FormSubmissionRepository submissionRepository;

    private final FormRepository formRepository;

    private final UserRepository userRepository;

    /** 학생: 폼 응답 제출 (1인 1회) */
    @Transactional
    public FormSubmissionResponseDTO submit(Long formId, FormSubmitDTO dto, Long userId) {
        FormEntity form = findFormOrThrow(formId);

        if (!form.isAcceptingSubmission()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지금은 응답을 받지 않는 폼입니다.");
        }
        if (submissionRepository.existsByFormIdAndUserId(formId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 응답을 제출한 폼입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 없습니다."));

        Map<Long, FormAnswerDTO> answerByQuestionId = groupByQuestionId(form, dto.getAnswers());

        FormSubmissionEntity submission = FormSubmissionEntity.builder()
                .form(form)
                .user(user)
                .build();

        for (FormQuestionEntity question : form.getQuestions()) {
            FormAnswerDTO answerDTO = answerByQuestionId.get(question.getId());

            if (answerDTO == null || isBlankAnswer(question, answerDTO)) {
                if (question.isRequired()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "필수 질문에 답변해주세요: " + question.getTitle());
                }
                continue;
            }

            submission.addAnswer(buildAnswer(question, answerDTO));
        }

        return FormSubmissionResponseDTO.from(submissionRepository.save(submission));
    }

    /** 선생님: 폼별 제출 목록 */
    @Transactional(readOnly = true)
    public List<FormSubmissionSummaryResponseDTO> listSubmissions(Long formId) {
        findFormOrThrow(formId);

        return submissionRepository.findByFormIdOrderBySubmittedAtDesc(formId).stream()
                .map(FormSubmissionSummaryResponseDTO::from)
                .toList();
    }

    /** 선생님: 제출 단건 상세 */
    @Transactional(readOnly = true)
    public FormSubmissionResponseDTO getSubmission(Long formId, Long submissionId) {
        FormSubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "제출된 응답을 찾을 수 없습니다."));

        if (!submission.getForm().getId().equals(formId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이 폼의 응답이 아닙니다.");
        }

        return FormSubmissionResponseDTO.from(submission);
    }

    /** 학생: 내가 제출한 응답 조회 */
    @Transactional(readOnly = true)
    public FormSubmissionResponseDTO getMySubmission(Long formId, Long userId) {
        return submissionRepository.findByFormIdAndUserId(formId, userId)
                .map(FormSubmissionResponseDTO::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아직 제출한 응답이 없습니다."));
    }

    /** 요청 답변을 질문 ID로 묶는다. 이 폼에 없는 질문이거나 중복 응답이면 거부한다. */
    private Map<Long, FormAnswerDTO> groupByQuestionId(FormEntity form, List<FormAnswerDTO> answers) {
        Set<Long> questionIds = form.getQuestions().stream()
                .map(FormQuestionEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, FormAnswerDTO> grouped = new LinkedHashMap<>();
        for (FormAnswerDTO answer : answers) {
            if (!questionIds.contains(answer.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이 폼에 없는 질문입니다: " + answer.getQuestionId());
            }
            if (grouped.put(answer.getQuestionId(), answer) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "같은 질문에 답변이 두 번 들어왔습니다: " + answer.getQuestionId());
            }
        }
        return grouped;
    }

    /** 답변이 비어 있는지 (선택 안 함 / 빈 문자열) */
    private boolean isBlankAnswer(FormQuestionEntity question, FormAnswerDTO dto) {
        if (question.getType().hasOptions()) {
            return dto.getOptionIds() == null || dto.getOptionIds().isEmpty();
        }
        return dto.getTextValue() == null || dto.getTextValue().isBlank();
    }

    private FormAnswerEntity buildAnswer(FormQuestionEntity question, FormAnswerDTO dto) {
        return question.getType().hasOptions()
                ? buildChoiceAnswer(question, dto)
                : buildTextAnswer(question, dto);
    }

    /** 선택형 답변: 고른 선택지가 이 질문의 것인지, 개수 제한을 지켰는지 검사한다 */
    private FormAnswerEntity buildChoiceAnswer(FormQuestionEntity question, FormAnswerDTO dto) {
        if (dto.getTextValue() != null && !dto.getTextValue().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "선택형 질문에는 직접 입력할 수 없습니다: " + question.getTitle());
        }

        List<Long> optionIds = dto.getOptionIds();
        if (!question.getType().allowsMultiple() && optionIds.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이 질문은 하나만 선택할 수 있습니다: " + question.getTitle());
        }

        Map<Long, FormQuestionOptionEntity> optionById = question.getOptions().stream()
                .collect(Collectors.toMap(FormQuestionOptionEntity::getId, Function.identity()));

        FormAnswerEntity answer = FormAnswerEntity.builder()
                .question(question)
                .build();

        Set<Long> chosen = new HashSet<>();
        for (Long optionId : optionIds) {
            FormQuestionOptionEntity option = optionById.get(optionId);
            if (option == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이 질문의 선택지가 아닙니다: " + question.getTitle());
            }
            if (!chosen.add(optionId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "같은 선택지를 중복해서 고를 수 없습니다: " + question.getTitle());
            }
            answer.addSelectedOption(FormAnswerOptionEntity.builder()
                    .option(option)
                    .build());
        }

        return answer;
    }

    /** 주관식 답변: 숫자·날짜 유형은 형식을 검사한다 */
    private FormAnswerEntity buildTextAnswer(FormQuestionEntity question, FormAnswerDTO dto) {
        if (dto.getOptionIds() != null && !dto.getOptionIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "주관식 질문에는 선택지를 보낼 수 없습니다: " + question.getTitle());
        }

        String text = dto.getTextValue().trim();
        validateTextFormat(question, text);

        return FormAnswerEntity.builder()
                .question(question)
                .textValue(text)
                .build();
    }

    private void validateTextFormat(FormQuestionEntity question, String text) {
        if (question.getType() == QuestionType.NUMBER) {
            try {
                new BigDecimal(text);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "숫자만 입력할 수 있습니다: " + question.getTitle());
            }
        } else if (question.getType() == QuestionType.DATE) {
            try {
                LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "날짜는 YYYY-MM-DD 형식으로 보내주세요: " + question.getTitle());
            }
        }
    }

    private FormEntity findFormOrThrow(Long formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폼을 찾을 수 없습니다."));
    }
}
