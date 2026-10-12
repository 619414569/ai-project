package com.aiproject.scaffold.common.api;

import com.aiproject.scaffold.common.web.RequestContext;

import java.time.Instant;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        Instant timestamp
) {
    private static final String SUCCESS_CODE = "OK";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "Success", data, RequestContext.traceId(), Instant.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null, RequestContext.traceId(), Instant.now());
    }
}
