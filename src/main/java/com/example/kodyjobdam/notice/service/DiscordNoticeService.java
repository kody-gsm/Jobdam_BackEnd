package com.example.kodyjobdam.notice.service;
import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;

@Service
public class DiscordNoticeService {

    private final JDA jda;
    private final String channelId;

    public DiscordNoticeService(
            @Value("${discord.bot.token}") String token,
            @Value("${discord.bot.channel-id}") String channelId) throws Exception {

        this.jda = JDABuilder.createDefault(token).build().awaitReady();
        this.channelId = channelId;
    }

    public void sendNotice(NoticeRequestDto dto) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            throw new RuntimeException("지정한 디스코드 채널을 찾을 수 없습니다.");
        }

        EmbedBuilder embed = new EmbedBuilder();

        // 제목 및 제목 클릭 시 이동할 링크 설정
        if (dto.getLink() != null && !dto.getLink().isBlank()) {
            embed.setTitle(dto.getTitle(), dto.getLink());
        } else {
            embed.setTitle(dto.getTitle());
        }

        // 내용 설정 (프론트에서 전송한 마크다운 원본 그대로 주입)
        embed.setDescription(dto.getContent());

        // 디자인 포인트 설정
        embed.setColor(new Color(88, 101, 242)); // 색 rgb값

        // 링크가 있다면 하단에 개별 필드로도 추가 안내
        if (dto.getLink() != null && !dto.getLink().isBlank()) {
            embed.addField("관련 링크", dto.getLink(), false);
        }

        // 메세지 전송
        channel.sendMessageEmbeds(embed.build()).queue();
    }
}