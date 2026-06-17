package com.example.kodyjobdam.interview.dto;

import com.example.kodyjobdam.interview.entity.Interview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class InterviewResponse {
    private Long id;
    private int roomNumber;
    private LocalDate reservationDate;
    private String studentNumber;
    private String name;
    private String status;

    public static InterviewResponse from(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .roomNumber(interview.getRoomNumber())
                .reservationDate(interview.getReservationDate())
                .studentNumber(interview.getUser().getStudent_number()) // User 엔티티의 필드명에 맞춤
                .name(interview.getUser().getName())
                .status(interview.getStatus().name())
                .build();
    }
}