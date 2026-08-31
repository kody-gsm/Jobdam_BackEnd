package com.example.kodyjobdam.notice.controller;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.notice.service.DiscordNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final DiscordNoticeService discordNoticeService;

    @PostMapping
    public ResponseEntity<String> createNotice(@RequestBody NoticeRequestDto requestDto) {
        // DB에 공지 저장 로직 작성

        // 디스코드 봇으로 메시지 전송
        discordNoticeService.sendNotice(requestDto);

        return ResponseEntity.ok("공지가 등록되었으며 디스코드로 발송되었습니다.");
    }
}