package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 공개된 글을 디스코드 공지 채널에 알린다. 발송이 실패해도 공개 자체는 막지 않는다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeAnnouncer {

    private final DiscordNoticeService discordNoticeService;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public void announce(String title, String content, String path) {
        NoticeRequestDto notice = new NoticeRequestDto();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setLink(frontendBaseUrl.replaceAll("/+$", "") + path);

        try {
            discordNoticeService.sendNotice(notice);
        } catch (RuntimeException e) {
            log.warn("디스코드 공지를 보내지 못했습니다. link={}", notice.getLink(), e);
        }
    }
}
