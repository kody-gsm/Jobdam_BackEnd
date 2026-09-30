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

    public static RecruitException forbidden(String message) {
        return new RecruitException(HttpStatus.FORBIDDEN, "RECRUIT_FORBIDDEN", message);
    }

    public static RecruitException tooManyRequests(String message) {
        return new RecruitException(HttpStatus.TOO_MANY_REQUESTS, "RECRUIT_TOO_MANY_REQUESTS", message);
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

    /**
     * 아직 공개되지 않아 학생에게 보이지 않는 공고.
     *
     * <p>프론트가 마감(RECRUIT_CLOSED)과 구분해 안내할 수 있도록 코드를 나눈다.</p>
     */
    public static RecruitException notPublished(HttpStatus status, String message) {
        return new RecruitException(status, "RECRUIT_NOT_PUBLISHED", message);
    }

    /** 서류 접수가 마감되어 학생에게 보이지 않는 공고. */
    public static RecruitException closed(HttpStatus status, String message) {
        return new RecruitException(status, "RECRUIT_CLOSED", message);
    }
}
