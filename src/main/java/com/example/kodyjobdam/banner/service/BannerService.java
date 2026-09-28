package com.example.kodyjobdam.banner.service;

import com.example.kodyjobdam.banner.dto.response.BannerResponseDTO;
import com.example.kodyjobdam.banner.entity.BannerEntity;
import com.example.kodyjobdam.banner.repository.BannerRepository;
import com.example.kodyjobdam.common.exception.BannerException;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BannerService {

    private static final int TITLE_MAX_LENGTH = 100;

    private static final int CONTENT_MAX_LENGTH = 500;

    private static final int LINK_MAX_LENGTH = 2048;

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final BannerRepository bannerRepository;

    private final UserRepository userRepository;

    private final BannerImageStorage bannerImageStorage;

    @Transactional
    public BannerResponseDTO saveCurrent(MultipartFile image,
                                         String title,
                                         String content,
                                         String link,
                                         Long teacherId) {
        if (image == null || image.isEmpty()) {
            throw BannerException.badRequest("배너 이미지를 첨부해주세요.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw BannerException.badRequest("배너 이미지는 png, jpg, webp, gif 형식만 업로드할 수 있습니다.");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> BannerException.notFound("회원이 없습니다."));

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw BannerException.badRequest("배너 이미지를 읽을 수 없습니다.");
        }

        bannerRepository.findByActiveTrue().forEach(BannerEntity::deactivate);

        BannerEntity entity = bannerRepository.save(BannerEntity.builder()
                .user(teacher)
                .title(normalize(title, TITLE_MAX_LENGTH, "배너 제목"))
                .content(normalize(content, CONTENT_MAX_LENGTH, "배너 내용"))
                .link(normalize(link, LINK_MAX_LENGTH, "배너 링크"))
                .imageUrl(bannerImageStorage.store(imageBytes, IMAGE_EXTENSIONS.getOrDefault(contentType, "")))
                .active(true)
                .build());

        return BannerResponseDTO.from(entity);
    }

    @Transactional(readOnly = true)
    public Optional<BannerResponseDTO> getCurrent() {
        return bannerRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(BannerResponseDTO::from);
    }

    private String normalize(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw BannerException.badRequest(fieldName + "은 " + maxLength + "자 이하로 입력해주세요.");
        }
        return normalized;
    }
}
