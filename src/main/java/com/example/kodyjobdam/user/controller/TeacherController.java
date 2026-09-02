package com.example.kodyjobdam.user.controller;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.TeacherResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeacherController {

    private final UserRepository userRepository;

    @GetMapping("/api/teachers")
    public List<TeacherResponseDTO> getTeachers() {
        return userRepository.findByRole(UserRole.TEACHER).stream()
                .map(TeacherResponseDTO::from)
                .toList();
    }
}
