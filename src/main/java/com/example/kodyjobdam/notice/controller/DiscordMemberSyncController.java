package com.example.kodyjobdam.notice.controller;

import com.example.kodyjobdam.notice.dto.DiscordMemberSyncResponse;
import com.example.kodyjobdam.notice.service.DiscordMemberSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/admin/discord/members")
@RequiredArgsConstructor
public class DiscordMemberSyncController {

    private final DiscordMemberSyncScheduler syncScheduler;

    @PostMapping("/sync")
    public CompletableFuture<ResponseEntity<DiscordMemberSyncResponse>> syncMembers() {
        return syncScheduler.requestFullSync().thenApply(ResponseEntity::ok);
    }
}
