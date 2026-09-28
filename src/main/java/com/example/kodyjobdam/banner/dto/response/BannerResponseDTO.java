package com.example.kodyjobdam.banner.dto.response;

import com.example.kodyjobdam.banner.entity.BannerEntity;

import java.time.LocalDateTime;

public record BannerResponseDTO(
        Long id,
        String title,
        String content,
        String link,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BannerResponseDTO from(BannerEntity entity) {
        return new BannerResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getLink(),
                entity.getImageUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
