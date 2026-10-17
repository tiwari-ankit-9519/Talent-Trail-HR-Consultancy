package com.abchr.platform.auth;

import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    private static final Logger LOG = LogManager.getLogger(AuthRateLimiter.class);

    private final int limit;
    private final Duration window;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public AuthRateLimiter(
            @Value("${app.auth.rate-limit-attempts:30}") int limit,
            @Value("${app.auth.rate-limit-window-minutes:10}") int windowMinutes) {
        this.limit = limit;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void check(String scope, String ip) {
        String key = scope + "|" + (ip == null ? "unknown" : ip);
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                LOG.warn("Auth rate limit hit scope={} ip={}", scope, ip);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.RATE_LIMITED,
                        "Too many attempts from this connection. Please try again later.");
            }
            deque.addLast(now);
        }
        if (hits.size() > 10_000) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(cutoff);
                }
            });
        }
    }
}
