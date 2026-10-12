package com.aiproject.scaffold.common.web;

import org.slf4j.MDC;

public final class RequestContext {
    public static final String TRACE_ID_KEY = "traceId";

    private RequestContext() {
    }

    public static String traceId() {
        return MDC.get(TRACE_ID_KEY);
    }
}
