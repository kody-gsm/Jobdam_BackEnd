package com.example.kodyjobdam.common.repository;

import java.time.LocalDate;

/** 예약이 걸린 시간(날짜·교시·담당 선생님). 일반·진로 상담이 함께 쓴다. */
public record ReservationSlot(LocalDate date, String period, Long teacherId) {
}
