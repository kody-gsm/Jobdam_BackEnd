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

    @GetMapping({"/student/teachers", "/api/teachers"})
    public List<TeacherResponse> readTeachers() {
        return teacherService.findTeachers();
    }
}
