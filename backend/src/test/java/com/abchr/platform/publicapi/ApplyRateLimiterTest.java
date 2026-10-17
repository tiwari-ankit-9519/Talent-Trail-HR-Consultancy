package com.abchr.platform.publicapi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.abchr.platform.shared.web.ApiException;
import org.junit.jupiter.api.Test;

class ApplyRateLimiterTest {

    @Test
    void blocksAfterLimit() {
        ApplyRateLimiter limiter = new ApplyRateLimiter(2);
        limiter.check("1.1.1.1");
        limiter.check("1.1.1.1");
        assertThrows(ApiException.class, () -> limiter.check("1.1.1.1"));
    }

    @Test
    void separatesVisitors() {
        ApplyRateLimiter limiter = new ApplyRateLimiter(1);
        limiter.check("1.1.1.1");
        assertDoesNotThrow(() -> limiter.check("2.2.2.2"));
    }
}
