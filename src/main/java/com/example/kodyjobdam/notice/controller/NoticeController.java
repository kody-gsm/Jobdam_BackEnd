package com.example.kodyjobdam.notice.controller;

import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.notice.dto.DiscordChannelResponse;
import com.example.kodyjobdam.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/channels")
    public ResponseEntity<List<DiscordChannelResponse>> getSendableChannels() {
        return ResponseEntity.ok(noticeService.getSendableChannels());
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendToChannel(@Valid @RequestBody NoticeRequestDto requestDto) {
        return ResponseEntity.ok(noticeService.sendNoticeToChannel(requestDto));
    }

    @PostMapping
    public ResponseEntity<String> createNotice(@Valid @RequestBody NoticeRequestDto requestDto) {
        String messageId = noticeService.createNotice(requestDto);
        return ResponseEntity.ok(messageId);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<String> updateNotice(@PathVariable String messageId,
                                               @RequestParam(required = false) String channelId,
                                               @Valid @RequestBody NoticeRequestDto requestDto) {
        noticeService.updateNotice(messageId, channelId, requestDto);
        return ResponseEntity.ok("디스코드 공지가 수정되었습니다.");
    }
}
