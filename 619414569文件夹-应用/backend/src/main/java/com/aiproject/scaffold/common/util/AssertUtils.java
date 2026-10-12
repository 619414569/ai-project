package com.aiproject.scaffold.common.util;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;

public final class AssertUtils {
    private AssertUtils() {
    }

    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    public static <T> T notNull(T value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value;
    }

    public static String notBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value;
    }
}
