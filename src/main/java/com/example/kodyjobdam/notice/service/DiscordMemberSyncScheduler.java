package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.DiscordMemberSyncResponse;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordMemberSyncScheduler {

    private final DiscordNoticeService discordNoticeService;
    private final DiscordMemberLinkService linkService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "discord-member-sync");
        thread.setDaemon(true);
        return thread;
    });
    private CompletableFuture<DiscordMemberSyncResponse> inFlight;
    private volatile boolean initialized;
    private final List<MemberChange> pendingChanges = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        syncMembers();
    }

    /** 누락된 이벤트와 DB 학생 정보 변경을 바로잡는 저빈도 전체 점검. */
    @Scheduled(cron = "${discord.member-sync.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void syncMembers() {
        requestFullSync().whenComplete((result, error) -> {
            if (error != null) log.error("디스코드 서버 멤버 동기화 실패", error);
        });
    }

    /** 관리자 버튼에서도 사용한다. 실제 DB 반영이 끝난 뒤 결과가 반환된다. */
    public synchronized CompletableFuture<DiscordMemberSyncResponse> requestFullSync() {
        if (inFlight != null && !inFlight.isDone()) return inFlight;

        CompletableFuture<DiscordMemberSyncResponse> result = new CompletableFuture<>();
        inFlight = result;
        try {
            Guild guild = discordNoticeService.getJda().getGuildById(discordNoticeService.getGuildId());
            if (guild == null) throw new IllegalStateException("디스코드 서버를 찾을 수 없습니다.");
            guild.loadMembers()
                    .onSuccess(ignored -> executor.execute(() -> {
                        try {
                            DiscordMemberSyncResponse response = linkService.syncMembers(List.copyOf(guild.getMembers()));
                            List<MemberChange> replay;
                            synchronized (this) {
                                initialized = true;
                                replay = List.copyOf(pendingChanges);
                                pendingChanges.clear();
                            }
                            replay.forEach(this::applyChange);
                            result.complete(response);
                        } catch (RuntimeException e) {
                            result.completeExceptionally(e);
                        }
                    }))
                    .onError(result::completeExceptionally);
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    /** JDA 이벤트 스레드를 DB 조회로 막지 않고, 전체 조회와 순서대로 처리한다. */
    public void reconcileMemberChange(Guild guild, String discordId, String oldNickname,
                                      String newNickname, boolean present) {
        MemberChange change = new MemberChange(guild, discordId, oldNickname, newNickname, present);
        synchronized (this) {
            if (!initialized) {
                pendingChanges.add(change);
                return;
            }
        }
        executor.execute(() -> applyChange(change));
    }

    private void applyChange(MemberChange change) {
        try {
            linkService.reconcileMemberChange(change.guild(), change.discordId(),
                    change.oldNickname(), change.newNickname(), change.present());
        } catch (RuntimeException e) {
            log.error("디스코드 멤버 변경 반영 실패: 멤버={}", change.discordId(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private record MemberChange(Guild guild, String discordId, String oldNickname,
                                String newNickname, boolean present) {
    }
}
