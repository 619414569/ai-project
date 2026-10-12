package com.aiproject.scaffold.common.util;

import java.util.UUID;

public final class IdUtils {
    private IdUtils() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
