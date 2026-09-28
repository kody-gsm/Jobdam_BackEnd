package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class BannerException extends BusinessException {

    private BannerException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static BannerException badRequest(String message) {
        return new BannerException(HttpStatus.BAD_REQUEST, "BANNER_BAD_REQUEST", message);
    }

    public static BannerException notFound(String message) {
        return new BannerException(HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", message);
    }
}
