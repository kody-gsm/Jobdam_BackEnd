package com.example.kodyjobdam.user.controller;

import com.example.kodyjobdam.user.service.DataGsmStudentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class DataGsmStudentController {

    private final DataGsmStudentSyncService dataGsmStudentSyncService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> syncStudents() {
        int syncedCount = dataGsmStudentSyncService.syncAllStudents();
        return ResponseEntity.ok(Map.of("syncedCount", syncedCount));
    }
}
