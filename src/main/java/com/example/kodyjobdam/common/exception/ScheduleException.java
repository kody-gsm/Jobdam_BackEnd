package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class ScheduleException extends BusinessException {

    private ScheduleException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static ScheduleException badRequest(String message) {
        return new ScheduleException(HttpStatus.BAD_REQUEST, "SCHEDULE_BAD_REQUEST", message);
    }

    public static ScheduleException badGateway(String message) {
        return new ScheduleException(HttpStatus.BAD_GATEWAY, "SCHEDULE_UPSTREAM_ERROR", message);
    }
}
