package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordMemberLinkServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks DiscordMemberLinkService linkService;

    @Test
    void 학번과_이름이_정확히_일치하면_디스코드_ID를_연결한다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").build();
        Member member = member("1234 | 홍길동", "987654321");
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student));

        linkService.syncMembers(List.of(member));

        assertThat(student.getDiscordUserId()).isEqualTo("987654321");
    }

    @Test
    void 같은_학번과_이름을_쓰는_멤버가_둘이면_연결하지_않는다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").discordUserId("old-id").build();
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student));

        linkService.syncMembers(List.of(member("1234홍길동", "first"), member("1234-홍길동", "second")));

        assertThat(student.getDiscordUserId()).isNull();
    }

    @Test
    void 닉네임_형식이_다르면_연결하지_않는다() {
        assertThat(DiscordMemberLinkService.identityFromNickname("123홍길동")).isNull();
        assertThat(DiscordMemberLinkService.identityFromNickname("1234홍길동")).isEqualTo("1234:홍길동");
    }

    @Test
    void 연결이_그대로면_학생_행을_갱신하지_않는다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").discordUserId("987654321").build();
        when(userRepository.findByRole(UserRole.STUDENT)).thenReturn(List.of(student));

        linkService.syncMembers(List.of(member("1234홍길동", "987654321")));

        assertThat(student.getDiscordUserId()).isEqualTo("987654321");
        verify(userRepository, never()).flush();
    }

    @Test
    void 입장_이벤트는_해당_학생만_연결한다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").build();
        Guild guild = mock(Guild.class);
        when(guild.getMembers()).thenReturn(List.of());
        when(userRepository.findStudentsByIdentity("1234", "홍길동")).thenReturn(List.of(student));

        linkService.reconcileMemberChange(guild, "new-member", null, "1234홍길동", true);

        assertThat(student.getDiscordUserId()).isEqualTo("new-member");
    }

    @Test
    void 닉네임_변경은_이전_연결을_해제하고_새_학생에게_연결한다() {
        User oldStudent = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").discordUserId("member-id").build();
        User newStudent = User.builder().id(2L).role(UserRole.STUDENT)
                .student_number("5678").name("김철수").build();
        Guild guild = mock(Guild.class);
        when(guild.getMembers()).thenReturn(List.of());
        when(userRepository.findStudentsByIdentity("1234", "홍길동")).thenReturn(List.of(oldStudent));
        when(userRepository.findStudentsByIdentity("5678", "김철수")).thenReturn(List.of(newStudent));

        linkService.reconcileMemberChange(guild, "member-id", "1234홍길동", "5678김철수", true);

        assertThat(oldStudent.getDiscordUserId()).isNull();
        assertThat(newStudent.getDiscordUserId()).isEqualTo("member-id");
    }

    @Test
    void 중복_닉네임이_되면_기존_연결도_해제한다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").discordUserId("first").build();
        Guild guild = mock(Guild.class);
        Member first = member("1234홍길동", "first");
        when(guild.getMembers()).thenReturn(List.of(first));
        when(userRepository.findStudentsByIdentity("1234", "홍길동")).thenReturn(List.of(student));

        linkService.reconcileMemberChange(guild, "second", null, "1234홍길동", true);

        assertThat(student.getDiscordUserId()).isNull();
    }

    @Test
    void 중복_멤버가_나가면_남은_멤버에게_다시_연결한다() {
        User student = User.builder().id(1L).role(UserRole.STUDENT)
                .student_number("1234").name("홍길동").build();
        Guild guild = mock(Guild.class);
        Member remaining = member("1234홍길동", "remaining");
        when(guild.getMembers()).thenReturn(List.of(remaining));
        when(userRepository.findStudentsByIdentity("1234", "홍길동")).thenReturn(List.of(student));

        linkService.reconcileMemberChange(guild, "leaving", "1234홍길동", null, false);

        assertThat(student.getDiscordUserId()).isEqualTo("remaining");
    }

    private Member member(String nickname, String id) {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn(nickname);
        when(member.getId()).thenReturn(id);
        return member;
    }
}
