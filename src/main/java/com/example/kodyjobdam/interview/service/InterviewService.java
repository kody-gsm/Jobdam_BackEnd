package com.example.kodyjobdam.interview.service;

import com.example.kodyjobdam.interview.dto.InterviewRequest;
import com.example.kodyjobdam.interview.dto.InterviewResponse;
import com.example.kodyjobdam.interview.entity.Interview;
import com.example.kodyjobdam.interview.entity.ReservationStatus;
import com.example.kodyjobdam.interview.repository.InterviewRepository;
import com.example.kodyjobdam.user.entity.User;
import com.example.kodyjobdam.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    // 1. 면접실 예약 로직
    @Transactional
    public List<InterviewResponse> createReservation(InterviewRequest request, String email) {
        // 1-1. 현재 로그인한 학생 정보 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 🔥 [새로운 규칙 1] 기존에 예약한 기록(RESERVED)이 있다면 추가 예약 불가
        boolean hasExistingReservation = interviewRepository.existsByUserAndStatus(user, ReservationStatus.RESERVED);
        if (hasExistingReservation) {
            throw new IllegalArgumentException("이미 활성화된 면접실 예약 내역이 존재하여 추가 예약이 불가능합니다.");
        }

        // 🔥 [새로운 규칙 2] 예약 날짜 연속성 검증
        List<LocalDate> requestDates = request.getReservationDates();
        // 혹시 순서가 섞여서 들어올 수 있으므로 오름차순 정렬
        List<LocalDate> sortedDates = requestDates.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < sortedDates.size() - 1; i++) {
            LocalDate current = sortedDates.get(i);
            LocalDate next = sortedDates.get(i + 1);

            // 현재 날짜의 다음 날이 그다음 원소와 일치하지 않으면 연속되지 않은 것임
            if (!current.plusDays(1).equals(next)) {
                throw new IllegalArgumentException("면접실 예약은 반드시 연속된 날짜로만 신청할 수 있습니다.");
            }
        }

        List<Interview> interviewsToSave = new ArrayList<>();

        // 1-2. 요소를 순회하며 방 중복 체크 및 엔티티 생성
        for (LocalDate date : sortedDates) {

            boolean isAlreadyReserved = interviewRepository.existsByRoomNumberAndReservationDateAndStatus(
                    request.getRoomNumber(), date, ReservationStatus.RESERVED);

            if (isAlreadyReserved) {
                throw new IllegalArgumentException(date + " 날짜에 " + request.getRoomNumber() + "번 면접실은 이미 다른 학생이 예약했습니다.");
            }

            Interview interview = Interview.builder()
                    .user(user)
                    .roomNumber(request.getRoomNumber())
                    .reservationDate(date)
                    .status(ReservationStatus.RESERVED)
                    .build();

            interviewsToSave.add(interview);
        }

        // 1-3. 일괄 저장
        interviewRepository.saveAll(interviewsToSave);

        return interviewsToSave.stream()
                .map(InterviewResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 예약 취소 로직
    @Transactional
    public void cancelReservation(Long reservationId, String email) {
        Interview interview = interviewRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약을 찾을 수 없습니다."));

        if (!interview.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다.");
        }

        if (interview.getStatus() == ReservationStatus.CANCELED) {
            throw new IllegalArgumentException("이미 취소 처리된 예약입니다.");
        }

        interview.cancel();
    }

    // 3. 학생 본인의 예약 목록 조회 로직
    public List<InterviewResponse> getMyReservations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Interview> myInterviews = interviewRepository.findAllByUserAndStatus(user, ReservationStatus.RESERVED);

        return myInterviews.stream()
                .map(InterviewResponse::from)
                .collect(Collectors.toList());
    }
}