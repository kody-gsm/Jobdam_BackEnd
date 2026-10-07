package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.notice.dto.DiscordMemberSyncResponse;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordMemberLinkService {

    private static final Pattern NICKNAME = Pattern.compile("^([0-9]{4}).*([^\\s]{3})$");
    private final UserRepository userRepository;

    /** 전체 멤버 목록을 받은 경우에만 연결 상태를 갱신한다. 중복된 이름은 연결하지 않는다. */
    @Transactional
    public DiscordMemberSyncResponse syncMembers(List<Member> members) {
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        Map<String, User> studentsByIdentity = new HashMap<>();
        Set<String> duplicateStudents = new HashSet<>();
        for (User student : students) {
            String key = identity(student.getStudent_number(), student.getName());
            if (key == null || studentsByIdentity.putIfAbsent(key, student) != null) {
                duplicateStudents.add(key);
            }
        }

        Map<String, String> idsByIdentity = new HashMap<>();
        Set<String> duplicateMembers = new HashSet<>();
        for (Member member : members) {
            String key = identityFromNickname(member.getNickname());
            if (key != null && idsByIdentity.putIfAbsent(key, member.getId()) != null) {
                duplicateMembers.add(key);
            }
        }

        Map<User, String> changed = new HashMap<>();
        int linked = 0;
        for (User student : students) {
            String key = identity(student.getStudent_number(), student.getName());
            String desired = key != null && !duplicateStudents.contains(key) && !duplicateMembers.contains(key)
                    ? idsByIdentity.get(key) : null;
            if (desired != null) {
                linked++;
            }
            if (!Objects.equals(student.getDiscordUserId(), desired)) {
                changed.put(student, desired);
            }
        }

        int updated = changed.size();
        applyChanges(changed);
        log.info("디스코드 멤버 {}명 조회, 학생 {}명 연결, {}명 갱신", members.size(), linked, updated);
        return new DiscordMemberSyncResponse(members.size(), linked, updated);
    }

    /** 입장·닉네임 변경·퇴장 시 이전/현재 학번·이름에 해당하는 학생만 다시 연결한다. */
    @Transactional
    public void reconcileMemberChange(Guild guild, String discordId, String oldNickname,
                                      String newNickname, boolean present) {
        String oldKey = identityFromNickname(oldNickname);
        String newKey = present ? identityFromNickname(newNickname) : null;
        Set<String> affected = new LinkedHashSet<>();
        if (oldKey != null) affected.add(oldKey);
        if (newKey != null) affected.add(newKey);
        if (affected.isEmpty()) return;

        Map<String, List<String>> idsByIdentity = new HashMap<>();
        affected.forEach(key -> idsByIdentity.put(key, new ArrayList<>()));
        for (Member member : guild.getMembers()) {
            if (discordId.equals(member.getId())) continue;
            String key = identityFromNickname(member.getNickname());
            if (idsByIdentity.containsKey(key)) idsByIdentity.get(key).add(member.getId());
        }
        // 이벤트 중에는 JDA 캐시가 변경 전/후 어느 상태여도 같은 결과가 나오게 한다.
        if (newKey != null) idsByIdentity.get(newKey).add(discordId);

        Map<User, String> changed = new HashMap<>();
        for (String key : affected) {
            String[] parts = key.split(":", 2);
            List<User> students = userRepository.findStudentsByIdentity(parts[0], parts[1]);
            List<String> ids = idsByIdentity.get(key);
            String desired = students.size() == 1 && ids.size() == 1 ? ids.get(0) : null;
            for (User student : students) {
                if (!Objects.equals(student.getDiscordUserId(), desired)) {
                    changed.put(student, desired);
                }
            }
        }
        applyChanges(changed);
    }

    private void applyChanges(Map<User, String> changed) {
        // ID가 다른 학생에게 옮겨갈 때 unique 제약에 걸리지 않도록 변경 대상만 먼저 비운다.
        if (!changed.isEmpty()) {
            for (User student : changed.keySet()) {
                student.setDiscordUserId(null);
            }
            userRepository.flush();
            changed.forEach(User::setDiscordUserId);
        }
    }

    static String identityFromNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        Matcher matcher = NICKNAME.matcher(nickname.trim());
        return matcher.matches() ? identity(matcher.group(1), matcher.group(2)) : null;
    }

    private static String identity(String number, String name) {
        if (number == null || name == null || !number.matches("[0-9]{4}") || name.length() != 3) {
            return null;
        }
        return number + ":" + name;
    }
}
