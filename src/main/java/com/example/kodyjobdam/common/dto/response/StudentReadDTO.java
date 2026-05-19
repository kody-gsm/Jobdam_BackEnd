package com.example.kodyjobdam.common.dto.response;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class StudentReadDTO {

    private Long id;

    private String name;

    private LocalDate date;

    private String period;

    public StudentReadDTO(Long id, String name, LocalDate date, String period) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.period = period;
    }
}
