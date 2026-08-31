package com.example.kodyjobdam.common.exception;

import org.springframework.http.HttpStatus;

public class ConfigException extends BusinessException {

    private ConfigException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static ConfigException serviceUnavailable(String message) {
        return new ConfigException(HttpStatus.SERVICE_UNAVAILABLE, "CONFIGURATION_ERROR", message);
    }
}
