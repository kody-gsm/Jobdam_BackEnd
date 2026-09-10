package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.service.FormService;
import com.example.kodyjobdam.recruit.client.GeminiAnalysisResult;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.dto.RecruitPeriodDTO;
import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.notification.entity.NotificationType;
import com.example.kodyjobdam.notification.service.NotificationExpirationService;
import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/heic", "image/heif");

    private final RecruitRepository recruitRepository;

    private final UserRepository userRepository;

    private final GeminiClient geminiClient;

    private final FormService formService;

    private final NotificationService notificationService;

    private final NotificationExpirationService notificationExpirationService;

    /** 선생님: 이미지 분석 → 초안(DRAFT)으로 저장 후 결과 반환 */
    @Transactional
    public RecruitResponseDTO analyze(MultipartFile image, Long userId) {
        if (image == null || image.isEmpty()) {
            throw RecruitException.badRequest("이미지를 첨부해주세요.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw RecruitException.badRequest("이미지 파일만 업로드할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> RecruitException.notFound("회원이 없습니다."));

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw RecruitException.badRequest("이미지를 읽을 수 없습니다.");
        }

        GeminiAnalysisResult result = geminiClient.analyze(imageBytes, contentType);

        // 공고를 만들 때 학번·이름·포트폴리오로 이루어진 기본 지원 폼도 함께 만들어 붙인다.
        FormEntity form = formService.createForRecruit(
                user, result.companyName(), applicationDeadline(result.documentPeriod()));

        RecruitEntity entity = recruitRepository.save(RecruitEntity.builder()
                .user(user)
                .companyName(result.companyName())
                .documentPeriod(result.documentPeriod())
                .writtenExamPeriod(result.writtenExamPeriod())
                .practicalExamPeriod(result.practicalExamPeriod())
                .codingTestPeriod(result.codingTestPeriod())
                .interviewPeriod(result.interviewPeriod())
                .form(form)
                .summary(result.summary())
                .status(RecruitStatus.DRAFT)
                .build());

        return RecruitResponseDTO.from(entity);
    }

    /**
     * 선생님: 분석 결과 수정.
     * 요청에 담기지 않은(null) 항목은 기존 값을 유지한다. 값을 지울 때는 빈 문자열이나 빈 기간을 보낸다.
     */
    @Transactional
    public RecruitResponseDTO update(Long recruitId, RecruitUpdateDTO dto, Long teacherId) {
        RecruitEntity entity = findOrThrow(recruitId);
        validateOwner(entity, teacherId);
        entity.update(
                dto.getCompanyName() == null ? entity.getCompanyName() : dto.getCompanyName(),
                resolveDocumentPeriod(entity, dto),
                resolvePeriod(dto.getWrittenExamPeriod(), entity.getWrittenExamPeriod()),
                resolvePeriod(dto.getPracticalExamPeriod(), entity.getPracticalExamPeriod()),
                resolvePeriod(dto.getCodingTestPeriod(), entity.getCodingTestPeriod()),
                resolveInterviewPeriod(entity, dto),
                dto.getSummary() == null ? entity.getSummary() : dto.getSummary());
        return RecruitResponseDTO.from(entity);
    }

    /** 지원 폼 마감은 서류 접수 마지막 날 자정으로 잡는다. 접수 기간을 못 읽었으면 비워둔다. */
    private LocalDateTime applicationDeadline(RecruitPeriod documentPeriod) {
        LocalDate endDate = documentPeriod == null ? null : documentPeriod.getEndDate();
        return endDate == null ? null : endDate.atTime(LocalTime.MAX);
    }

    /** 요청에 없는 전형 기간은 기존 값을 그대로 둔다. */
    private RecruitPeriod resolvePeriod(RecruitPeriodDTO requested, RecruitPeriod current) {
        if (requested == null) {
            return current;
        }

        RecruitPeriod period = RecruitPeriodDTO.toPeriod(requested);
        return period.isEmpty() ? null : period;
    }

    /** 지원 마감(deadline)은 서류 접수 종료일만 바꾼다. documentPeriod를 함께 보내면 그쪽을 따른다. */
    private RecruitPeriod resolveDocumentPeriod(RecruitEntity entity, RecruitUpdateDTO dto) {
        RecruitPeriod current = entity.getDocumentPeriod();
        if (dto.getDocumentPeriod() != null || dto.getDeadline() == null) {
            return resolvePeriod(dto.getDocumentPeriod(), current);
        }

        RecruitPeriod deadline = RecruitPeriod.parse(dto.getDeadline());
        LocalDate startDate = current == null ? null : current.getStartDate();
        LocalDate endDate = deadline == null ? null : deadline.getEndDate();
        if (startDate == null && endDate == null) {
            return null;
        }

        return new RecruitPeriod(startDate, endDate);
    }

    /** 면접 일정(interviewDate)은 기간 전체를 바꾼다. interviewPeriod를 함께 보내면 그쪽을 따른다. */
    private RecruitPeriod resolveInterviewPeriod(RecruitEntity entity, RecruitUpdateDTO dto) {
        if (dto.getInterviewPeriod() != null || dto.getInterviewDate() == null) {
            return resolvePeriod(dto.getInterviewPeriod(), entity.getInterviewPeriod());
        }

        return RecruitPeriod.parse(dto.getInterviewDate());
    }

    /** 선생님: 채용 공고 삭제 (공개된 공고도 지울 수 있다) */
    @Transactional
    public void delete(Long recruitId, Long teacherId) {
        RecruitEntity entity = findOrThrow(recruitId);
        validateOwner(entity, teacherId);

        Long formId = entity.getFormId();
        // 공고가 폼을 참조하므로 공고를 먼저 지운 뒤에 폼을 정리한다.
        recruitRepository.delete(entity);
        recruitRepository.flush();
        if (formId != null) {
            formService.deleteForRecruit(formId);
        }
    }

    /** 선생님: 학생에게 공개 */
    @Transactional
    public RecruitResponseDTO publish(Long recruitId, Long teacherId) {
        RecruitEntity entity = findOrThrow(recruitId);
        validateOwner(entity, teacherId);
        if (entity.getStatus() != RecruitStatus.DRAFT) {
            throw RecruitException.badRequest("초안 상태의 채용 공고만 공개할 수 있습니다.");
        }

        entity.publish();
        // 공고가 열리면 지원 폼도 함께 열어 학생이 바로 지원할 수 있게 한다.
        if (entity.getFormId() != null) {
            formService.publishForRecruit(entity.getFormId());
        }
        notificationService.notifyAllStudents(
                NotificationType.RECRUIT_PUBLISHED,
                "새로운 취업 공지",
                entity.getCompanyName() + " 취업 공지가 게시되었습니다.",
                entity.getId(),
                "/recruit/" + entity.getId(),
                notificationExpirationService.recruitExpiresAt(entity.getDeadline())
        );
        return RecruitResponseDTO.from(entity);
    }

    /** 선생님 관리용: 초안 포함 전체 목록 */
    public List<RecruitResponseDTO> listForTeacher(Long teacherId) {
        return recruitRepository.findByUserIdOrderByCreatedAtDesc(teacherId).stream()
                .map(RecruitResponseDTO::from)
                .toList();
    }

    /** 학생/공개용: 공개된 공고 목록 */
    public List<RecruitResponseDTO> listPublished() {
        return recruitRepository.findByStatusOrderByCreatedAtDesc(RecruitStatus.PUBLISHED).stream()
                .map(RecruitResponseDTO::from)
                .toList();
    }

    /** 학생/공개용: 공개된 공고 단건 */
    public RecruitResponseDTO getPublished(Long recruitId) {
        RecruitEntity entity = findOrThrow(recruitId);
        if (entity.getStatus() != RecruitStatus.PUBLISHED) {
            throw RecruitException.notFound("공개된 공고가 아닙니다.");
        }
        return RecruitResponseDTO.from(entity);
    }

    private RecruitEntity findOrThrow(Long recruitId) {
        return recruitRepository.findById(recruitId)
                .orElseThrow(() -> RecruitException.notFound("채용 공고를 찾을 수 없습니다."));
    }

    private void validateOwner(RecruitEntity entity, Long teacherId) {
        if (entity.getUser() == null || !entity.getUser().getId().equals(teacherId)) {
            throw RecruitException.forbidden("채용 공고를 관리할 권한이 없습니다.");
        }
    }
}
