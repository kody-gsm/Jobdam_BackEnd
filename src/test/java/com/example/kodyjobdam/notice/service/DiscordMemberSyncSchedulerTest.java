package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.DiscordMemberSyncResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.concurrent.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordMemberSyncSchedulerTest {

    @Mock DiscordNoticeService discordNoticeService;
    @Mock DiscordMemberLinkService linkService;
    private DiscordMemberSyncScheduler scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) scheduler.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 관리자_전체_조회는_DB_동기화가_끝난_뒤_결과를_돌려준다() throws Exception {
        scheduler = new DiscordMemberSyncScheduler(discordNoticeService, linkService);
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        Task<List<Member>> loadTask = mock(Task.class);
        DiscordMemberSyncResponse summary = new DiscordMemberSyncResponse(2, 1, 1);
        when(discordNoticeService.getJda()).thenReturn(jda);
        when(discordNoticeService.getGuildId()).thenReturn("guild-id");
        when(jda.getGuildById("guild-id")).thenReturn(guild);
        when(guild.loadMembers()).thenReturn(loadTask);
        when(guild.getMembers()).thenReturn(List.of(mock(Member.class), mock(Member.class)));
        when(linkService.syncMembers(any())).thenReturn(summary);
        doAnswer(invocation -> {
            Consumer<List<Member>> callback = invocation.getArgument(0);
            callback.accept(List.of());
            return loadTask;
        }).when(loadTask).onSuccess(any());
        when(loadTask.onError(any())).thenReturn(loadTask);

        CompletableFuture<DiscordMemberSyncResponse> result = scheduler.requestFullSync();

        assertThat(result.get(3, TimeUnit.SECONDS)).isEqualTo(summary);
        verify(linkService).syncMembers(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 최초_전체_조회_중_들어온_변경_이벤트도_반영한다() throws Exception {
        scheduler = new DiscordMemberSyncScheduler(discordNoticeService, linkService);
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        Task<List<Member>> loadTask = mock(Task.class);
        when(discordNoticeService.getJda()).thenReturn(jda);
        when(discordNoticeService.getGuildId()).thenReturn("guild-id");
        when(jda.getGuildById("guild-id")).thenReturn(guild);
        when(guild.loadMembers()).thenReturn(loadTask);
        when(guild.getMembers()).thenReturn(List.of());
        when(linkService.syncMembers(any())).thenReturn(new DiscordMemberSyncResponse(0, 0, 0));
        doAnswer(invocation -> {
            Consumer<List<Member>> callback = invocation.getArgument(0);
            callback.accept(List.of());
            return loadTask;
        }).when(loadTask).onSuccess(any());
        when(loadTask.onError(any())).thenReturn(loadTask);

        scheduler.reconcileMemberChange(guild, "member-id", null, "1234홍길동", true);
        scheduler.requestFullSync().get(3, TimeUnit.SECONDS);

        verify(linkService).reconcileMemberChange(guild, "member-id", null, "1234홍길동", true);
    }
}
