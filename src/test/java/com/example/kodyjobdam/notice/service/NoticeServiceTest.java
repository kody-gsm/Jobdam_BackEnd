package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitField;
import com.example.kodyjobdam.recruit.repository.RecruitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private DiscordNoticeService discordNoticeService;

    @Mock
    private RecruitRepository recruitRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void 공고_링크가_이미_디스코드_메시지와_연결되어_있으면_새로_보내지_않고_수정한다() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(30L)
                .companyName("testify")
                .summary("요약")
                .discordMessageId("1234567890")
                .build();
        recruit.replaceFields(List.of(RecruitField.ETC));
        when(recruitRepository.findById(30L)).thenReturn(Optional.of(recruit));

        NoticeRequestDto dto = notice("testify 공고", "요약", "https://jobdom-front-uen1.vercel.app/recruit/30");

        String messageId = noticeService.createNotice(dto);

        assertThat(messageId).isEqualTo("1234567890");
        ArgumentCaptor<NoticeRequestDto> captor = ArgumentCaptor.forClass(NoticeRequestDto.class);
        verify(discordNoticeService).updateNotice(org.mockito.ArgumentMatchers.eq("1234567890"), captor.capture());
        assertThat(captor.getValue().getField()).isEqualTo("기타");
        verify(discordNoticeService, never()).sendNotice(any());
    }

    @Test
    void 공고_링크지만_저장된_메시지_ID가_없으면_보낸_뒤_공고에_연결한다() {
        RecruitEntity recruit = RecruitEntity.builder()
                .id(30L)
                .companyName("testify")
                .summary("요약")
                .build();
        when(recruitRepository.findById(30L)).thenReturn(Optional.of(recruit));
        when(discordNoticeService.sendNotice(any())).thenReturn("1234567890");

        String messageId = noticeService.createNotice(
                notice("testify 공고", "요약", "https://jobdom-front-uen1.vercel.app/recruit/30"));

        assertThat(messageId).isEqualTo("1234567890");
        assertThat(recruit.getDiscordMessageId()).isEqualTo("1234567890");
    }

    @Test
    void 공고_링크가_아니면_그대로_새_공지로_보낸다() {
        when(discordNoticeService.sendNotice(any())).thenReturn("999");

        String messageId = noticeService.createNotice(
                notice("일반 공지", "내용", "https://jobdom-front-uen1.vercel.app/forms/10"));

        assertThat(messageId).isEqualTo("999");
        verify(recruitRepository, never()).findById(any());
    }

    private NoticeRequestDto notice(String title, String content, String link) {
        NoticeRequestDto dto = new NoticeRequestDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setLink(link);
        return dto;
    }
}
