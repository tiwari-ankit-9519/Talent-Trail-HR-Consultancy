package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.auth.rate-limit-attempts=3")
class AuthRateLimitIT extends IntegrationTestBase {

    @Test
    void repeatedSignInAttemptsFromOneAddressGetTooManyRequests() {
        int last = 0;
        for (int i = 0; i < 6; i++) {
            last = post("/api/v1/auth/login", null,
                    "{\"email\":\"nobody" + i + "@talenttrail.test\",\"password\":\"Wrong-Passw0rd!\"}").status();
        }
        assertEquals(429, last);
    }
}
