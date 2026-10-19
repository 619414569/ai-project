package com.aiproject.scaffold.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DEFAULT_ZONE);

    private DateTimeUtils() {
    }

    public static String format(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant);
    }

    public static Instant now() {
        return Instant.now();
    }
}
