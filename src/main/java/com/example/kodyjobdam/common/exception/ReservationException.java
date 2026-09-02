package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class ReservationException extends BusinessException {

    private ReservationException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static ReservationException badRequest(String message) {
        return new ReservationException(HttpStatus.BAD_REQUEST, "RESERVATION_BAD_REQUEST", message);
    }

    public static ReservationException notFound(String message) {
        return new ReservationException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", message);
    }

    public static ReservationException forbidden(String message) {
        return new ReservationException(HttpStatus.FORBIDDEN, "RESERVATION_FORBIDDEN", message);
    }

    public static ReservationException conflict(String message) {
        return new ReservationException(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", message);
    }

    public static ReservationException locked(String message) {
        return new ReservationException(HttpStatus.LOCKED, "RESERVATION_LOCKED", message);
    }
}
