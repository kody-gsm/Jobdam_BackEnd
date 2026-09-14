package com.example.kodyjobdam.common.dto.response;

/** 학생 상담 목록에 내려주는 예약 진행 상태. 일반·진로 상담이 함께 쓴다. */
public enum ReservationStatus {
    WAITING,
    RESERVED,
    CANCELED;

    /** 일반(common)·진로(course) 예약 상태를 학생 화면용 상태로 바꾼다. */
    public static ReservationStatus from(Enum<?> state) {
        return switch (state.name()) {
            case "WAITING" -> WAITING;
            case "RESERVED" -> RESERVED;
            // 학생 예약이 잠금·자동 상태가 되는 경로는 없다. 유효한 예약이 아니므로 취소로 본다.
            default -> CANCELED;
        };
    }
}
