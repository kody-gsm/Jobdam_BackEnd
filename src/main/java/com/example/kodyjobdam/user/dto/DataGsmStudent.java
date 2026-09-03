package com.example.kodyjobdam.user.dto;

public record DataGsmStudent(
        Long id,
        String name,
        String email,
        Integer grade,
        Integer classNum,
        Integer number,
        Integer studentNumber
) {
    public String studentNumberAsString() {
        return studentNumber == null ? null : String.format("%04d", studentNumber);
    }
}
