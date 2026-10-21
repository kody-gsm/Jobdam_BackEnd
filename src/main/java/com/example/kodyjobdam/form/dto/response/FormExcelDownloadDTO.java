package com.example.kodyjobdam.form.dto.response;

/** 폼 응답 엑셀 내려받기. 파일 이름과 xlsx 내용을 담는다. */
public record FormExcelDownloadDTO(String fileName, byte[] content) {
}
