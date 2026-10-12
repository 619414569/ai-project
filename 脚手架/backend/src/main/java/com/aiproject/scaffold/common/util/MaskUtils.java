package com.aiproject.scaffold.common.util;

public final class MaskUtils {
    private MaskUtils() {
    }

    public static String mobile(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() < 7) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 3)
                + "*".repeat(value.length() - 7)
                + value.substring(value.length() - 4);
    }

    public static String email(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int separator = value.indexOf('@');
        if (separator <= 1) {
            return "***" + (separator >= 0 ? value.substring(separator) : "");
        }
        return value.charAt(0) + "***" + value.substring(separator);
    }
}
