package com.example.kodyjobdam.recruit.dto.response;

import org.springframework.core.io.Resource;

/** 채용 공고 이미지 조회에 필요한 값만 담는다 */
public record RecruitImageDownloadDTO(Resource resource, String contentType) {
}
