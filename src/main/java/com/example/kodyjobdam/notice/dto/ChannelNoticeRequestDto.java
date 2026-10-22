package com.example.kodyjobdam.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChannelNoticeRequestDto extends NoticeRequestDto {
    @NotBlank(message = "전송할 디스코드 채널을 선택해주세요.")
    private String channelId;
}
