package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.DataGsmStudent;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DataGsmStudentSyncService {

    private static final int DEFAULT_PAGE_SIZE = 300;

    private final DataGsmStudentClient dataGsmStudentClient;
    private final UserRepository userRepository;

    public int syncAllStudents() {
        int savedCount = 0;
        int page = 0;

        while (true) {
            List<DataGsmStudent> students = dataGsmStudentClient.fetchStudents(page, DEFAULT_PAGE_SIZE);
            if (students.isEmpty()) {
                return savedCount;
            }

            for (DataGsmStudent student : students) {
                upsertStudent(student);
                savedCount++;
            }

            if (students.size() < DEFAULT_PAGE_SIZE) {
                return savedCount;
            }

            page++;
        }
    }

    public User syncStudentByEmail(String email) {
        DataGsmStudent student = dataGsmStudentClient.fetchStudentByEmail(email);
        if (student == null) {
            return null;
        }

        return upsertStudent(student);
    }

    private User upsertStudent(DataGsmStudent student) {
        User user = userRepository.findByDataGsmStudentId(student.id())
                .or(() -> userRepository.findByEmail(student.email()))
                .orElseGet(User::new);

        user.setDataGsmStudentId(student.id());
        user.setName(student.name());
        user.setEmail(student.email());
        user.setStudent_number(student.studentNumberAsString());
        user.setGrade(student.grade());
        user.setClassNum(student.classNum());
        user.setNumber(student.number());
        user.setRole(UserRole.STUDENT);

        return userRepository.save(user);
    }
}
