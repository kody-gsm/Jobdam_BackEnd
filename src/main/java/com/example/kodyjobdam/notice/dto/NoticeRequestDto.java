package com.example.kodyjobdam.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeRequestDto {
    @NotBlank(message = "공지 제목을 입력해주세요.")
    @Size(max = 256, message = "공지 제목은 256자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "공지 내용을 입력해주세요.")
    @Size(max = 4096, message = "공지 내용은 4096자 이하로 입력해주세요.")
    private String content;

    @Size(max = 2048, message = "공지 링크는 2048자 이하로 입력해주세요.")
    private String link;
}
