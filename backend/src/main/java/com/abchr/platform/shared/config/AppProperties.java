package com.abchr.platform.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Security security,
        @Valid @NotNull Auth auth,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Storage storage,
        @Valid @NotNull Mail mail,
        @Valid @NotNull Outbox outbox,
        @Valid @NotNull Cache cache) {

    public record Security(
            @NotBlank @Size(min = 32) String jwtSecret,
            @Min(1) int jwtAccessTtlMinutes,
            @Min(1) int jwtRefreshTtlDays) {
    }

    public record Auth(
            @Min(1) int maxFailedAttempts,
            @Min(1) int lockMinutes,
            boolean cookieSecure,
            String bootstrapAdminEmail,
            String bootstrapAdminPassword) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record Storage(@NotBlank String basePath) {
    }

    public record Mail(@NotBlank String from) {
    }

    public record Outbox(
            @NotBlank String exchange,
            @Min(100) long pollIntervalMs,
            @Min(1) @Max(500) int batchSize) {
    }

    public record Cache(@Min(1) long defaultTtlSeconds) {
    }
}
