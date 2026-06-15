package com.example.kodyjobdam.interview.repository;

import com.example.kodyjobdam.interview.entity.Interview;
import com.example.kodyjobdam.interview.entity.ReservationStatus;
import com.example.kodyjobdam.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    boolean existsByRoomNumberAndReservationDateAndStatus(int roomNumber, LocalDate date, ReservationStatus status);

    List<Interview> findAllByUserAndStatus(User user, ReservationStatus status);

    //해당 학생이 이미 예약(RESERVED)해 둔 기록이 있는지 확인
    boolean existsByUserAndStatus(User user, ReservationStatus status);
}