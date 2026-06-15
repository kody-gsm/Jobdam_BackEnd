package com.example.kodyjobdam.interview.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class InterviewRequest {

    @NotNull(message = "면접실 번호는 필수입니다.")
    private int roomNumber;

    @NotNull(message = "예약 날짜는 필수입니다.")
    @Size(min = 1, max = 3, message = "예약은 최대 3일까지 가능합니다.")
    private List<LocalDate> reservationDates;
}