package com.abchr.platform.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthRateLimiterTest {

    @Test
    void blocksAfterLimitWithTooManyRequests() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, 10);
        for (int i = 0; i < 3; i++) {
            limiter.check("login", "1.1.1.1");
        }
        ApiException ex = assertThrows(ApiException.class, () -> limiter.check("login", "1.1.1.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals(ErrorCodes.RATE_LIMITED, ex.getCode());
    }

    @Test
    void countsEachIpSeparately() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 10);
        limiter.check("login", "1.1.1.1");
        assertDoesNotThrow(() -> limiter.check("login", "2.2.2.2"));
    }

    @Test
    void countsEachScopeSeparately() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 10);
        limiter.check("login", "1.1.1.1");
        assertDoesNotThrow(() -> limiter.check("refresh", "1.1.1.1"));
    }

    @Test
    void handlesMissingIp() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 10);
        limiter.check("login", null);
        assertThrows(ApiException.class, () -> limiter.check("login", null));
    }
}
