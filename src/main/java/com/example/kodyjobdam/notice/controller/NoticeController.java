package com.example.kodyjobdam.notice.controller;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.notice.service.DiscordNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final DiscordNoticeService discordNoticeService;

    @PostMapping
    public ResponseEntity<String> createNotice(@Valid @RequestBody NoticeRequestDto requestDto) {
        // DB에 공지 저장 로직 작성

        // 디스코드 봇으로 메시지 전송
        String messageId = discordNoticeService.sendNotice(requestDto);

        return ResponseEntity.ok(messageId);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<String> updateNotice(@PathVariable String messageId,
                                               @Valid @RequestBody NoticeRequestDto requestDto) {
        discordNoticeService.updateNotice(messageId, requestDto);
        return ResponseEntity.ok("디스코드 공지가 수정되었습니다.");
    }
}
