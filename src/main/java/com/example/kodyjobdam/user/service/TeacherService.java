package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.TeacherResponse;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;

    /** 상담 종류를 가리지 않는 선생님 전체 목록 */
    public List<TeacherResponse> findTeachers() {
        return toResponses(userRepository.findByRoleInOrderByNameAsc(
                List.of(UserRole.TEACHER, UserRole.WEE_TEACHER)));
    }

    /** 일반(common) 상담을 담당하는 Wee 클래스 선생님 목록 */
    public List<TeacherResponse> findCommonTeachers() {
        return toResponses(userRepository.findByRoleOrderByNameAsc(UserRole.WEE_TEACHER));
    }

    /** 진로(course) 상담을 담당하는 선생님 목록 */
    public List<TeacherResponse> findCourseTeachers() {
        return toResponses(userRepository.findByRoleOrderByNameAsc(UserRole.TEACHER));
    }

    private List<TeacherResponse> toResponses(List<User> users) {
        return users.stream()
                .map(user -> new TeacherResponse(user.getId(), user.getName()))
                .toList();
    }
}
