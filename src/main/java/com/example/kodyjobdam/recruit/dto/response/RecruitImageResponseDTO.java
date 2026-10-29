package com.example.kodyjobdam.recruit.dto.response;

import com.example.kodyjobdam.recruit.entity.RecruitImageEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitImageResponseDTO {

    /** 이미지 식별자. 이미지를 지울 때 쓴다. */
    private Long id;

    /** 공개 조회 URL. 로그인 없이도 접근할 수 있다. */
    private String url;

    public static RecruitImageResponseDTO from(RecruitImageEntity entity) {
        return RecruitImageResponseDTO.builder()
                .id(entity.getId())
                .url(entity.getImageUrl())
                .build();
    }
}
