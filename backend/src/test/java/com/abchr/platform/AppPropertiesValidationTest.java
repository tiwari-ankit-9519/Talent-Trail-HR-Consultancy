package com.abchr.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.abchr.platform.shared.config.AppProperties;

class AppPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AppProperties valid() {
        return new AppProperties(
                new AppProperties.Security("a".repeat(48), 15, 7),
                new AppProperties.Auth(5, 15, false, "", ""),
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Storage("/data/files"),
                new AppProperties.Mail("noreply@example.com"),
                new AppProperties.Outbox("abchr.events", 2000, 50),
                new AppProperties.Cache(600));
    }

    @Test
    void acceptsCompleteConfiguration() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void rejectsShortJwtSecret() {
        Set<ConstraintViolation<AppProperties.Security>> violations = validator
                .validate(new AppProperties.Security("short", 15, 7));
        assertEquals(1, violations.size());
    }

    @Test
    void rejectsBlankStoragePath() {
        Set<ConstraintViolation<AppProperties.Storage>> violations = validator.validate(new AppProperties.Storage(" "));
        assertEquals(1, violations.size());
    }

    @Test
    void rejectsEmptyOrigins() {
        Set<ConstraintViolation<AppProperties.Cors>> violations = validator.validate(new AppProperties.Cors(List.of()));
        assertEquals(1, violations.size());
    }
}
