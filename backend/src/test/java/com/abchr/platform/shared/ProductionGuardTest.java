package com.abchr.platform.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.abchr.platform.shared.config.ProductionGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionGuardTest {

    private MockEnvironment good() {
        return new MockEnvironment()
                .withProperty("APP_ENV", "production")
                .withProperty("app.auth.cookie-secure", "true")
                .withProperty("app.security.jwt-secret", "j".repeat(40))
                .withProperty("app.security.field-key", "f".repeat(40))
                .withProperty("spring.datasource.password", "db-pass-9f3k")
                .withProperty("spring.data.redis.password", "redis-pass-1x")
                .withProperty("spring.rabbitmq.password", "rabbit-pass-7q")
                .withProperty("app.cors.allowed-origins", "https://hr.example.com");
    }

    @Test
    void acceptsSafeProductionSettings() {
        new ProductionGuard(good()).afterPropertiesSet();
    }

    @Test
    void ignoresUnsafeSettingsOutsideProduction() {
        MockEnvironment env = good().withProperty("APP_ENV", "local").withProperty("app.auth.cookie-secure", "false");
        new ProductionGuard(env).afterPropertiesSet();
    }

    @Test
    void rejectsInsecureCookiesExampleSecretsAndHttpOrigins() {
        MockEnvironment env = good()
                .withProperty("app.auth.cookie-secure", "false")
                .withProperty("app.security.jwt-secret", "change-me-to-a-random-string-of-at-least-32-characters")
                .withProperty("spring.datasource.password", "change-me-db-password")
                .withProperty("app.cors.allowed-origins", "http://localhost");
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> new ProductionGuard(env).afterPropertiesSet());
        String message = failure.getMessage();
        assertTrue(message.contains("AUTH_COOKIE_SECURE"), message);
        assertTrue(message.contains("JWT_SECRET"), message);
        assertTrue(message.contains("DB_PASSWORD"), message);
        assertTrue(message.contains("CORS_ALLOWED_ORIGINS"), message);
    }

    @Test
    void rejectsSameKeyForSigningAndEncryption() {
        MockEnvironment env = good().withProperty("app.security.field-key", "j".repeat(40));
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> new ProductionGuard(env).afterPropertiesSet());
        assertEquals(1, failure.getMessage().split("must be different", -1).length - 1);
    }
}
