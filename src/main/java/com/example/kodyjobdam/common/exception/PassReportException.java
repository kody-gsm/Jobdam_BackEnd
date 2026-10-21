package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class PassReportException extends BusinessException {

    private PassReportException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static PassReportException badRequest(String message) {
        return new PassReportException(HttpStatus.BAD_REQUEST, "PASS_REPORT_BAD_REQUEST", message);
    }

    public static PassReportException notFound(String message) {
        return new PassReportException(HttpStatus.NOT_FOUND, "PASS_REPORT_NOT_FOUND", message);
    }
}
