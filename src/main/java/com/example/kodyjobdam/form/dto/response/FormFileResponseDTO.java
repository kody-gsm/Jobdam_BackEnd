package com.example.kodyjobdam.form.dto.response;

import com.example.kodyjobdam.form.entity.FormFileEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 파일 업로드 결과. 여기서 받은 id를 답변의 fileId로 보내 제출한다. */
@Getter
@Builder
public class FormFileResponseDTO {

    private Long id;

    private String originalName;

    private String contentType;

    private long size;

    /** 내려받기 주소 */
    private String downloadUrl;

    private LocalDateTime uploadedAt;

    public static FormFileResponseDTO from(FormFileEntity entity) {
        return FormFileResponseDTO.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .downloadUrl(downloadUrl(entity.getId()))
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    public static String downloadUrl(Long fileId) {
        return "/form/file/" + fileId;
    }
}
