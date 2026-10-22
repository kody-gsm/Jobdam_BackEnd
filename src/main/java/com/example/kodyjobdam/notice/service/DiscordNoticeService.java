package com.example.kodyjobdam.notice.service;
import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.notice.dto.DiscordChannelResponse;
import com.example.kodyjobdam.common.exception.NotificationException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DiscordNoticeService {

    private final JDA jda;
    private final String channelId;

    @Autowired
    public DiscordNoticeService(
            @Value("${discord.bot.token}") String token,
            @Value("${discord.bot.channel-id}") String channelId) throws Exception {
        this(JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build().awaitReady(), channelId);
    }

    DiscordNoticeService(JDA jda, String channelId) {
        this.jda = jda;
        this.channelId = channelId;
    }

    public String sendNotice(NoticeRequestDto dto) {
        TextChannel channel = getNoticeChannel();
        Message message = channel.sendMessageEmbeds(buildNoticeEmbed(dto).build()).complete();
        return message.getId();
    }

    public List<DiscordChannelResponse> getSendableChannels() {
        var guild = getNoticeChannel().getGuild();
        return Stream.<StandardGuildMessageChannel>concat(
                        guild.getTextChannels().stream(), guild.getNewsChannels().stream())
                .filter(this::canSendNotice)
                .map(channel -> new DiscordChannelResponse(
                        channel.getId(), channel.getName(),
                        channel.getParentCategory() == null ? null : channel.getParentCategory().getName()))
                .toList();
    }

    public String sendNoticeToChannel(String selectedChannelId, NoticeRequestDto dto) {
        var guildChannel = jda.getGuildChannelById(selectedChannelId);
        if (!(guildChannel instanceof StandardGuildMessageChannel channel)
                || !channel.getGuild().getId().equals(getGuildId()) || !canSendNotice(channel)) {
            throw NotificationException.badRequest("선택한 디스코드 채널에 공지를 보낼 수 없습니다.");
        }
        return channel.sendMessageEmbeds(buildNoticeEmbed(dto).build()).complete().getId();
    }

    private boolean canSendNotice(StandardGuildMessageChannel channel) {
        return channel.canTalk()
                && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
    }

    public void updateNotice(String messageId, NoticeRequestDto dto) {
        TextChannel channel = getNoticeChannel();
        channel.editMessageEmbedsById(messageId, buildNoticeEmbed(dto).build()).complete();
    }

    private TextChannel getNoticeChannel() {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            throw new RuntimeException("지정한 디스코드 채널을 찾을 수 없습니다.");
        }

        return channel;
    }

    public JDA getJda() {
        return jda;
    }

    public String getGuildId() {
        return getNoticeChannel().getGuild().getId();
    }

    public void sendDirectMessage(String userId, String content) {
        jda.openPrivateChannelById(userId)
                .flatMap(channel -> channel.sendMessage(content))
                .complete();
    }

    private EmbedBuilder buildNoticeEmbed(NoticeRequestDto dto) {
        EmbedBuilder embed = new EmbedBuilder();

        // 제목 및 제목 클릭 시 이동할 링크 설정
        if (dto.getLink() != null && !dto.getLink().isBlank()) {
            embed.setTitle(dto.getTitle(), dto.getLink());
        } else {
            embed.setTitle(dto.getTitle());
        }

        // 내용 설정 (프론트에서 전송한 마크다운 원본 그대로 주입)
        embed.setDescription(dto.getContent());

        if (dto.getField() != null && !dto.getField().isBlank()) {
            embed.addField("분야", dto.getField(), false);
        }

        // 디자인 포인트 설정
        embed.setColor(new Color(88, 101, 242)); // 색 rgb값

        // 링크가 있다면 하단에 개별 필드로도 추가 안내
        if (dto.getLink() != null && !dto.getLink().isBlank()) {
            embed.addField("관련 링크", dto.getLink(), false);
        }

        return embed;
    }
}
