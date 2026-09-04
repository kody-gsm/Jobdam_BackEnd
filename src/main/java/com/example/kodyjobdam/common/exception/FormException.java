package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class FormException extends BusinessException {

    private FormException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static FormException badRequest(String message) {
        return new FormException(HttpStatus.BAD_REQUEST, "FORM_BAD_REQUEST", message);
    }

    public static FormException notFound(String message) {
        return new FormException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", message);
    }

    public static FormException conflict(String message) {
        return new FormException(HttpStatus.CONFLICT, "FORM_CONFLICT", message);
    }

    public static FormException forbidden(String message) {
        return new FormException(HttpStatus.FORBIDDEN, "FORM_FORBIDDEN", message);
    }
}
