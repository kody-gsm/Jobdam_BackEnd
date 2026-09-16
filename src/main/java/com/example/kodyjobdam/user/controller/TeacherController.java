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

    /** 선생님 전체 목록 */
    @GetMapping({"/student/teachers", "/api/teachers"})
    public List<TeacherResponse> readTeachers() {
        return teacherService.findTeachers();
    }

    /** 일반 상담에서 고를 수 있는 선생님 목록 */
    @GetMapping("/student/common/teachers")
    public List<TeacherResponse> readCommonTeachers() {
        return teacherService.findCommonTeachers();
    }

    /** 진로 상담에서 고를 수 있는 선생님 목록 */
    @GetMapping("/student/course/teachers")
    public List<TeacherResponse> readCourseTeachers() {
        return teacherService.findCourseTeachers();
    }
}
