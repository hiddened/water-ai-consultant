package com.waterai.consultant.common.trace;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TraceIdProvider {

    public String currentTraceId() {
        return Optional.ofNullable(MDC.get(TraceIdFilter.TRACE_ID_KEY)).orElse("unknown");
    }
}

