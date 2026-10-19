package com.aiproject.scaffold.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed", HttpStatus.BAD_REQUEST),
    BUSINESS_ERROR("BUSINESS_ERROR", "Business rule validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Access is denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "Resource was not found", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "Resource state conflict", HttpStatus.CONFLICT),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Dependent service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }
}
