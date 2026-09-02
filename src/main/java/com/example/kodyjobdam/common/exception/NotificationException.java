package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class NotificationException extends BusinessException {

    private NotificationException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static NotificationException notFound(String message) {
        return new NotificationException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", message);
    }

    public static NotificationException badRequest(String message) {
        return new NotificationException(HttpStatus.BAD_REQUEST, "NOTIFICATION_BAD_REQUEST", message);
    }
}
