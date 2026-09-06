package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.recruit.client.GeminiAnalysisResult;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.dto.request.RecruitUpdateDTO;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
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

    private final NotificationService notificationService;

    private final NotificationExpirationService notificationExpirationService;

    /** 선생님: 이미지 분석 → 초안(DRAFT)으로 저장 후 결과 반환 */
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

        RecruitEntity entity = recruitRepository.save(RecruitEntity.builder()
                .user(user)
                .companyName(result.companyName())
                .interviewDate(result.interviewDate())
                .deadline(result.deadline())
                .summary(result.summary())
                .status(RecruitStatus.DRAFT)
                .build());

        return RecruitResponseDTO.from(entity);
    }

    /** 선생님: 분석 결과 수정 */
    @Transactional
    public RecruitResponseDTO update(Long recruitId, RecruitUpdateDTO dto, Long teacherId) {
        RecruitEntity entity = findOrThrow(recruitId);
        validateOwner(entity, teacherId);
        entity.update(dto.getCompanyName(), dto.getInterviewDate(), dto.getDeadline(), dto.getSummary());
        return RecruitResponseDTO.from(entity);
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
