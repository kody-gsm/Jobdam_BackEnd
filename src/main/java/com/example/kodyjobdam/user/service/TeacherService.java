package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.TeacherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;

    public List<TeacherResponse> findTeachers() {
        return userRepository.findByRoleOrderByNameAsc(UserRole.TEACHER).stream()
                .map(user -> new TeacherResponse(user.getId(), user.getName()))
                .toList();
    }
}
