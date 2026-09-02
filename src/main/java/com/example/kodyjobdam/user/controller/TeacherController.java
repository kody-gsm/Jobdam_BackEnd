package com.example.kodyjobdam.user.controller;

import com.example.kodyjobdam.user.dto.TeacherResponse;
import com.example.kodyjobdam.user.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    // 학생이 상담을 신청할 선생님을 고르기 위한 목록
    @GetMapping("/student/teachers")
    public List<TeacherResponse> readTeachers() {
        return teacherService.findTeachers();
    }
}
