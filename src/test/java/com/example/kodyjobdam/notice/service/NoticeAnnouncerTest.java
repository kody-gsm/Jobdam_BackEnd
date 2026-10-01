package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeAnnouncerTest {

    @Mock
    private DiscordNoticeService discordNoticeService;

    @InjectMocks
    private NoticeAnnouncer noticeAnnouncer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noticeAnnouncer, "frontendBaseUrl", "https://jobdam.test/");
    }

    @Test
    void 프론트_주소를_붙여_절대_링크로_보낸다() {
        noticeAnnouncer.announce("잡담 공고", "요약", "/recruit/10");

        ArgumentCaptor<NoticeRequestDto> captor = ArgumentCaptor.forClass(NoticeRequestDto.class);
        verify(discordNoticeService).sendNotice(captor.capture());
        NoticeRequestDto notice = captor.getValue();
        assertThat(notice.getTitle()).isEqualTo("잡담 공고");
        assertThat(notice.getContent()).isEqualTo("요약");
        assertThat(notice.getField()).isNull();
        assertThat(notice.getLink()).isEqualTo("https://jobdam.test/recruit/10");
    }

    @Test
    void 분야를_함께_보낸다() {
        noticeAnnouncer.announce("잡담 공고", "요약", "백엔드, AI", "/recruit/10");

        ArgumentCaptor<NoticeRequestDto> captor = ArgumentCaptor.forClass(NoticeRequestDto.class);
        verify(discordNoticeService).sendNotice(captor.capture());
        assertThat(captor.getValue().getField()).isEqualTo("백엔드, AI");
    }

    @Test
    void 기존_메시지를_수정한다() {
        noticeAnnouncer.update("1234567890", "잡담 공고", "수정된 요약", "백엔드", "/recruit/10");

        ArgumentCaptor<NoticeRequestDto> captor = ArgumentCaptor.forClass(NoticeRequestDto.class);
        verify(discordNoticeService).updateNotice(org.mockito.ArgumentMatchers.eq("1234567890"), captor.capture());
        NoticeRequestDto notice = captor.getValue();
        assertThat(notice.getTitle()).isEqualTo("잡담 공고");
        assertThat(notice.getContent()).isEqualTo("수정된 요약");
        assertThat(notice.getField()).isEqualTo("백엔드");
        assertThat(notice.getLink()).isEqualTo("https://jobdam.test/recruit/10");
    }

    @Test
    void 발송이_실패해도_예외를_퍼뜨리지_않는다() {
        when(discordNoticeService.sendNotice(any())).thenThrow(new IllegalStateException("디스코드 오류"));

        assertThatCode(() -> noticeAnnouncer.announce("잡담 공고", "요약", "/recruit/10"))
                .doesNotThrowAnyException();
    }
}
