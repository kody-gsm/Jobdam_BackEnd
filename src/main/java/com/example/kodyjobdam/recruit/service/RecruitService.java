package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.recruit.client.GeminiAnalysisResult;
import com.example.kodyjobdam.recruit.client.GeminiClient;
import com.example.kodyjobdam.recruit.dto.response.RecruitResponseDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    public RecruitResponseDTO analyze(MultipartFile image, Long userId) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지를 첨부해주세요.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원이 없습니다."));

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지를 읽을 수 없습니다.");
        }

        GeminiAnalysisResult result = geminiClient.analyze(imageBytes, contentType);

        RecruitEntity entity = recruitRepository.save(RecruitEntity.builder()
                .user(user)
                .companyName(result.companyName())
                .interviewDate(result.interviewDate())
                .deadline(result.deadline())
                .summary(result.summary())
                .build());

        return RecruitResponseDTO.from(entity);
    }

    public List<RecruitResponseDTO> list(Long userId) {
        return recruitRepository.findByUser_idOrderByCreatedAtDesc(userId).stream()
                .map(RecruitResponseDTO::from)
                .toList();
    }
}
