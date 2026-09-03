package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class RecruitException extends BusinessException {

    private RecruitException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static RecruitException badRequest(String message) {
        return new RecruitException(HttpStatus.BAD_REQUEST, "RECRUIT_BAD_REQUEST", message);
    }

    public static RecruitException notFound(String message) {
        return new RecruitException(HttpStatus.NOT_FOUND, "RECRUIT_NOT_FOUND", message);
    }

    public static RecruitException badGateway(String message) {
        return new RecruitException(HttpStatus.BAD_GATEWAY, "RECRUIT_BAD_GATEWAY", message);
    }

    public static RecruitException unprocessableEntity(String message) {
        return new RecruitException(HttpStatus.UNPROCESSABLE_ENTITY, "RECRUIT_UNPROCESSABLE_ENTITY", message);
    }

    public static RecruitException internalServerError(String message) {
        return new RecruitException(HttpStatus.INTERNAL_SERVER_ERROR, "RECRUIT_INTERNAL_SERVER_ERROR", message);
    }
}
