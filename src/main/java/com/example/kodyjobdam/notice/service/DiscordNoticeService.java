package com.example.kodyjobdam.notice.service;
import com.example.kodyjobdam.notice.dto.NoticeRequestDto;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
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

    public String sendRecruit(RecruitEntity recruit) {
        TextChannel channel = getNoticeChannel();
        Message message = channel.sendMessageEmbeds(buildRecruitEmbed(recruit).build()).complete();
        return message.getId();
    }

    public void updateRecruit(String messageId, RecruitEntity recruit) {
        TextChannel channel = getNoticeChannel();
        channel.editMessageEmbedsById(messageId, buildRecruitEmbed(recruit).build()).complete();
    }

    private TextChannel getNoticeChannel() {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            throw new RuntimeException("지정한 디스코드 채널을 찾을 수 없습니다.");
        }

        return channel;
    }

    private EmbedBuilder buildRecruitEmbed(RecruitEntity recruit) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(recruit.getCompanyName() + " 채용 공고");
        embed.setDescription(blankToDefault(recruit.getSummary(), "공고 요약이 없습니다."));
        embed.setColor(new Color(88, 101, 242));
        embed.addField("서류 접수", displayPeriod(recruit.getDocumentPeriod()), true);
        embed.addField("필기 전형", displayPeriod(recruit.getWrittenExamPeriod()), true);
        embed.addField("실기 전형", displayPeriod(recruit.getPracticalExamPeriod()), true);
        embed.addField("코딩테스트", displayPeriod(recruit.getCodingTestPeriod()), true);
        embed.addField("면접", displayPeriod(recruit.getInterviewPeriod()), true);
        embed.addField("지원 링크", "/recruit/" + recruit.getId(), false);
        return embed;
    }

    private String displayPeriod(RecruitPeriod period) {
        if (period == null || period.isEmpty()) {
            return RecruitPeriod.UNDECIDED;
        }

        return period.toDisplay();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
