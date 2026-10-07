package com.example.kodyjobdam.notice.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscordMemberEventListener extends ListenerAdapter {

    private final DiscordNoticeService discordNoticeService;
    private final DiscordMemberSyncScheduler syncScheduler;

    @PostConstruct
    public void register() {
        discordNoticeService.getJda().addEventListener(this);
    }

    @PreDestroy
    public void unregister() {
        discordNoticeService.getJda().removeEventListener(this);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (isTargetGuild(event.getGuild().getId())) {
            syncScheduler.reconcileMemberChange(event.getGuild(), event.getUser().getId(),
                    null, event.getMember().getNickname(), true);
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (isTargetGuild(event.getGuild().getId())) {
            syncScheduler.reconcileMemberChange(event.getGuild(), event.getMember().getId(),
                    event.getOldValue(), event.getNewValue(), true);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (!isTargetGuild(event.getGuild().getId())) return;
        if (event.getMember() == null) {
            syncScheduler.syncMembers();
            return;
        }
        syncScheduler.reconcileMemberChange(event.getGuild(), event.getUser().getId(),
                event.getMember().getNickname(), null, false);
    }

    @Override
    public void onSessionResume(SessionResumeEvent event) {
        syncScheduler.syncMembers();
    }

    @Override
    public void onSessionRecreate(SessionRecreateEvent event) {
        syncScheduler.syncMembers();
    }

    private boolean isTargetGuild(String guildId) {
        return discordNoticeService.getGuildId().equals(guildId);
    }
}
