package com.example.kodyjobdam.form.dto.response;

import org.springframework.core.io.Resource;

/** 파일 내려받기에 필요한 값만 담는다 */
public record FormFileDownloadDTO(Resource resource, String originalName, String contentType, long size) {
}
