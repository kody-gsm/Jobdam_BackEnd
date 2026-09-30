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

    /**
     * 아직 공개되지 않아 학생에게 보이지 않는 폼.
     *
     * <p>조회는 폼의 존재 자체를 감추려고 404, 제출은 잘못된 요청이라 400을 쓴다.
     * 프론트가 마감(FORM_CLOSED)과 구분해 안내할 수 있도록 코드를 나눈다.</p>
     */
    public static FormException notPublished(HttpStatus status, String message) {
        return new FormException(status, "FORM_NOT_PUBLISHED", message);
    }

    /** 마감되어 더 이상 응답을 받지 않는 폼. 조회는 404, 제출은 400. */
    public static FormException closed(HttpStatus status, String message) {
        return new FormException(status, "FORM_CLOSED", message);
    }
}
