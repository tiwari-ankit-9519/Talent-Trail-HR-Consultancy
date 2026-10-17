package com.abchr.platform.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public record RequestInfo(String ipAddress, String userAgent, String requestId) {

    private static final RequestInfo EMPTY = new RequestInfo(null, null, null);

    public static RequestInfo current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            String requestId = ThreadContext.get(RequestIdFilter.MDC_KEY);
            return requestId == null ? EMPTY : new RequestInfo(null, null, requestId);
        }
        HttpServletRequest request = attributes.getRequest();
        return new RequestInfo(
                ClientIp.of(request),
                request.getHeader("User-Agent"),
                ThreadContext.get(RequestIdFilter.MDC_KEY));
    }
}
