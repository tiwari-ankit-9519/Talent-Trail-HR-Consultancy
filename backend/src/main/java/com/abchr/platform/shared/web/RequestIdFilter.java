package com.abchr.platform.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String MDC_USER_KEY = "userId";
    public static final String MDC_USER_EMAIL_KEY = "userEmail";

    private static final Logger ACCESS = LogManager.getLogger("access");
    private static final String[] HTTP_KEYS = {"httpMethod", "httpPath", "httpStatus", "durationMs", "errorCode"};

    private final long slowRequestMs;
    private final boolean bodyLogging;
    private final HttpBodyLogger bodyLogger;

    public RequestIdFilter(
            @Value("${app.logging.slow-request-ms:1000}") long slowRequestMs,
            @Value("${app.logging.http-bodies-enabled:false}") boolean bodyLogging,
            HttpBodyLogger bodyLogger) {
        this.slowRequestMs = slowRequestMs;
        this.bodyLogging = bodyLogging;
        this.bodyLogger = bodyLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && incoming.matches("[A-Za-z0-9\\-]{8,64}")
                ? incoming
                : UUID.randomUUID().toString();
        ThreadContext.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        ThreadContext.put("httpMethod", request.getMethod());
        ThreadContext.put("httpPath", request.getRequestURI());
        long started = System.nanoTime();
        Throwable failure = null;
        boolean capture = bodyLogging && !request.getRequestURI().startsWith("/actuator")
                && !request.getRequestURI().endsWith("/notifications/stream");
        HttpServletRequest in = capture ? new ContentCachingRequestWrapper(request, HttpBodyLogger.MAX_BYTES) : request;
        HttpServletResponse out = capture ? new ContentCachingResponseWrapper(response) : response;
        if (!request.getRequestURI().startsWith("/actuator")) {
            ACCESS.info("HTTP request started {} {} ip={}", request.getMethod(), request.getRequestURI(),
                    request.getRemoteAddr());
        }
        try {
            chain.doFilter(in, out);
        } catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        } finally {
            if (capture) {
                try {
                    bodyLogger.logRequest((ContentCachingRequestWrapper) in);
                    bodyLogger.logResponse((ContentCachingResponseWrapper) out);
                } catch (RuntimeException ex) {
                    ACCESS.warn("Could not log HTTP bodies: {}", ex.toString());
                }
            }
            logAccess(request, out, started, failure);
            if (capture) {
                ((ContentCachingResponseWrapper) out).copyBodyToResponse();
            }
            ThreadContext.remove(MDC_KEY);
            ThreadContext.remove(MDC_USER_KEY);
            ThreadContext.remove(MDC_USER_EMAIL_KEY);
            for (String key : HTTP_KEYS) {
                ThreadContext.remove(key);
            }
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long started, Throwable failure) {
        String path = request.getRequestURI();
        long millis = (System.nanoTime() - started) / 1_000_000;
        int status = failure != null ? 500 : response.getStatus();
        String query = request.getQueryString();
        String target = query == null ? path : path + "?" + query;
        Object code = request.getAttribute(RequestErrors.ATTR_CODE);
        Object exception = request.getAttribute(RequestErrors.ATTR_EXCEPTION);
        if (failure != null && code == null) {
            code = ErrorCodes.INTERNAL_ERROR;
            exception = failure.getClass().getName();
        }

        ThreadContext.put("httpMethod", request.getMethod());
        ThreadContext.put("httpPath", path);
        ThreadContext.put("httpStatus", String.valueOf(status));
        ThreadContext.put("durationMs", String.valueOf(millis));
        if (code != null) {
            ThreadContext.put("errorCode", String.valueOf(code));
        }

        String agent = request.getHeader("User-Agent");
        String detail = (code == null ? "" : " code=" + code)
                + (exception == null ? "" : " exception=" + exception)
                + (agent == null ? "" : " ua=\"" + (agent.length() > 120 ? agent.substring(0, 120) : agent) + "\"");
        String line = "{} {} -> {} in {} ms ip={}{}";

        boolean noisy = path.startsWith("/actuator");
        if (status >= 500) {
            ACCESS.error(line, request.getMethod(), target, status, millis, request.getRemoteAddr(), detail);
        } else if (status >= 400) {
            ACCESS.warn(line, request.getMethod(), target, status, millis, request.getRemoteAddr(), detail);
        } else if (millis >= slowRequestMs && !noisy) {
            ACCESS.warn("SLOW " + line, request.getMethod(), target, status, millis, request.getRemoteAddr(), detail);
        } else if (noisy) {
            ACCESS.debug(line, request.getMethod(), target, status, millis, request.getRemoteAddr(), detail);
        } else {
            ACCESS.info(line, request.getMethod(), target, status, millis, request.getRemoteAddr(), detail);
        }
    }
}
