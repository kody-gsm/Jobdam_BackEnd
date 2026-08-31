package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends BusinessException {

    private AuthException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static AuthException unauthorized(String message) {
        return new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", message);
    }
}
