package com.example.kodyjobdam.user.dto;

import com.example.kodyjobdam.user.entity.User;

public record TeacherResponseDTO(
        Long id,
        String name
) {

    public static TeacherResponseDTO from(User user) {
        return new TeacherResponseDTO(user.getId(), user.getName());
    }
}
