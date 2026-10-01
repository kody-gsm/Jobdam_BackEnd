package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitField;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final DiscordNoticeService discordNoticeService;
    private final RecruitRepository recruitRepository;

    @Transactional
    public String createNotice(NoticeRequestDto dto) {
        Optional<RecruitEntity> recruit = findRecruitByNoticeLink(dto.getLink());
        if (recruit.isPresent()) {
            RecruitEntity entity = recruit.get();
            applyRecruitField(dto, entity);
            if (entity.getDiscordMessageId() != null && !entity.getDiscordMessageId().isBlank()) {
                discordNoticeService.updateNotice(entity.getDiscordMessageId(), dto);
                return entity.getDiscordMessageId();
            }

            String messageId = discordNoticeService.sendNotice(dto);
            entity.linkDiscordMessage(messageId);
            return messageId;
        }

        return discordNoticeService.sendNotice(dto);
    }

    public void updateNotice(String messageId, NoticeRequestDto dto) {
        discordNoticeService.updateNotice(messageId, dto);
    }

    private Optional<RecruitEntity> findRecruitByNoticeLink(String link) {
        Long recruitId = extractRecruitId(link);
        if (recruitId == null) {
            return Optional.empty();
        }
        return recruitRepository.findById(recruitId);
    }

    private Long extractRecruitId(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }

        String path = link.trim();
        try {
            URI uri = URI.create(path);
            if (uri.getPath() != null) {
                path = uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
            // URI로 읽을 수 없는 값은 아래 경로 파싱에서 한 번 더 확인한다.
        }

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("recruit".equals(parts[i])) {
                try {
                    return Long.parseLong(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private void applyRecruitField(NoticeRequestDto dto, RecruitEntity recruit) {
        if (dto.getField() != null && !dto.getField().isBlank()) {
            return;
        }

        List<RecruitField> fields = recruit.getSortedFields();
        if (fields.isEmpty()) {
            dto.setField("미정");
            return;
        }
        dto.setField(fields.stream()
                .map(RecruitField::getDisplayName)
                .collect(Collectors.joining(", ")));
    }
}
