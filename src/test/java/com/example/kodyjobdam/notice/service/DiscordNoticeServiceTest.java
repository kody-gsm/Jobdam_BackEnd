package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.common.exception.NotificationException;
import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

class DiscordNoticeServiceTest {

    private final JDA jda = mock(JDA.class);
    private final Guild guild = mock(Guild.class);
    private final Member selfMember = mock(Member.class);
    private final TextChannel defaultChannel = mock(TextChannel.class);
    private final DiscordNoticeService service = new DiscordNoticeService(jda, "default");

    @BeforeEach
    void setUp() {
        when(jda.getTextChannelById("default")).thenReturn(defaultChannel);
        when(defaultChannel.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("target-guild");
    }

    @Test
    void 목록에는_공지_전송_권한이_있는_텍스트_채널만_포함한다() {
        TextChannel sendable = mock(TextChannel.class);
        TextChannel blocked = mock(TextChannel.class);
        when(guild.getTextChannels()).thenReturn(List.of(sendable, blocked));
        when(guild.getNewsChannels()).thenReturn(List.of());
        when(sendable.canTalk()).thenReturn(true);
        when(sendable.getGuild()).thenReturn(guild);
        when(guild.getSelfMember()).thenReturn(selfMember);
        when(selfMember.hasPermission(sendable, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(sendable.getId()).thenReturn("123");
        when(sendable.getName()).thenReturn("공지");

        assertThat(service.getSendableChannels()).extracting("id").containsExactly("123");
    }

    @Test
    void 다른_서버의_채널에는_보내지_않는다() {
        TextChannel otherChannel = mock(TextChannel.class);
        Guild otherGuild = mock(Guild.class);
        when(jda.getGuildChannelById("other")).thenReturn(otherChannel);
        when(otherChannel.getGuild()).thenReturn(otherGuild);
        when(otherGuild.getId()).thenReturn("other-guild");

        assertThatThrownBy(() -> service.sendNoticeToChannel("other", null))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void 선택한_채널에_공지를_보낸다() {
        TextChannel selectedChannel = mock(TextChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class);
        Message message = mock(Message.class);
        when(jda.getGuildChannelById("selected")).thenReturn(selectedChannel);
        when(selectedChannel.getGuild()).thenReturn(guild);
        when(selectedChannel.canTalk()).thenReturn(true);
        when(guild.getSelfMember()).thenReturn(selfMember);
        when(selfMember.hasPermission(selectedChannel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(selectedChannel.sendMessageEmbeds(any(net.dv8tion.jda.api.entities.MessageEmbed.class)))
                .thenReturn(action);
        when(action.complete()).thenReturn(message);
        when(message.getId()).thenReturn("sent-message");
        NoticeRequestDto notice = new NoticeRequestDto();
        notice.setTitle("제목");
        notice.setContent("내용");

        assertThat(service.sendNoticeToChannel("selected", notice)).isEqualTo("sent-message");
    }

    @Test
    void 기본_채널이_없어도_서버가_하나면_채널_목록을_조회한다() {
        when(jda.getTextChannelById("default")).thenReturn(null);
        when(jda.getGuilds()).thenReturn(List.of(guild));
        when(guild.getTextChannels()).thenReturn(List.of());
        when(guild.getNewsChannels()).thenReturn(List.of());

        assertThat(service.getSendableChannels()).isEmpty();
    }

    @Test
    void 설정된_서버_ID로_기본_채널_없이_선택_채널을_검증한다() {
        DiscordNoticeService configuredService = new DiscordNoticeService(jda, "missing", "target-guild");
        TextChannel selectedChannel = mock(TextChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class);
        Message message = mock(Message.class);
        NoticeRequestDto notice = notice();
        when(jda.getGuildById("target-guild")).thenReturn(guild);
        when(jda.getGuildChannelById("selected")).thenReturn(selectedChannel);
        when(selectedChannel.getGuild()).thenReturn(guild);
        when(selectedChannel.canTalk()).thenReturn(true);
        when(guild.getSelfMember()).thenReturn(selfMember);
        when(selfMember.hasPermission(selectedChannel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(selectedChannel.sendMessageEmbeds(any(net.dv8tion.jda.api.entities.MessageEmbed.class)))
                .thenReturn(action);
        when(action.complete()).thenReturn(message);
        when(message.getId()).thenReturn("sent-message");

        assertThat(configuredService.sendNoticeToChannel("selected", notice)).isEqualTo("sent-message");
        verify(jda, never()).getTextChannelById("missing");
    }

    @Test
    void 선택한_채널의_메시지를_같은_채널에서_수정한다() {
        TextChannel selectedChannel = mock(TextChannel.class);
        MessageEditAction action = mock(MessageEditAction.class);
        when(jda.getGuildChannelById("selected")).thenReturn(selectedChannel);
        when(selectedChannel.getGuild()).thenReturn(guild);
        when(selectedChannel.canTalk()).thenReturn(true);
        when(guild.getSelfMember()).thenReturn(selfMember);
        when(selfMember.hasPermission(selectedChannel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(selectedChannel.editMessageEmbedsById(org.mockito.ArgumentMatchers.eq("sent-message"),
                any(net.dv8tion.jda.api.entities.MessageEmbed.class))).thenReturn(action);

        service.updateNotice("sent-message", "selected", notice());

        verify(action).complete();
        verify(defaultChannel, never()).editMessageEmbedsById(
                org.mockito.ArgumentMatchers.anyString(), any(net.dv8tion.jda.api.entities.MessageEmbed.class));
    }

    @Test
    void 채널_ID가_비어_있으면_전송하지_않는다() {
        assertThatThrownBy(() -> service.sendNoticeToChannel(null, notice()))
                .isInstanceOf(NotificationException.class);
    }

    private NoticeRequestDto notice() {
        NoticeRequestDto notice = new NoticeRequestDto();
        notice.setTitle("제목");
        notice.setContent("내용");
        return notice;
    }
}
