package com.abchr.platform.shared.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionGuard implements InitializingBean {

    private static final Logger LOG = LogManager.getLogger(ProductionGuard.class);
    private static final Set<String> PRODUCTION_NAMES = Set.of("production", "prod");
    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "ChangeMe-First-Login-1";

    private final Environment environment;

    public ProductionGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isProduction(environment)) {
            return;
        }
        List<String> problems = problems(environment);
        if (!problems.isEmpty()) {
            problems.forEach(problem -> LOG.error("Unsafe production configuration: {}", problem));
            throw new IllegalStateException(
                    "Refusing to start with unsafe production settings: " + String.join("; ", problems));
        }
        LOG.info("Production configuration checks passed");
    }

    static boolean isProduction(Environment environment) {
        String name = environment.getProperty("APP_ENV", "local");
        return PRODUCTION_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    static List<String> problems(Environment environment) {
        List<String> problems = new ArrayList<>();
        if (!environment.getProperty("app.auth.cookie-secure", Boolean.class, true)) {
            problems.add("AUTH_COOKIE_SECURE must be true");
        }
        String jwt = environment.getProperty("app.security.jwt-secret", "");
        String fieldKey = environment.getProperty("app.security.field-key", "");
        checkSecret("JWT_SECRET", jwt, problems);
        checkSecret("FIELD_ENCRYPTION_KEY", fieldKey, problems);
        if (!jwt.isBlank() && jwt.equals(fieldKey)) {
            problems.add("JWT_SECRET and FIELD_ENCRYPTION_KEY must be different");
        }
        checkPassword("DB_PASSWORD", environment.getProperty("spring.datasource.password", ""), problems);
        checkPassword("REDIS_PASSWORD", environment.getProperty("spring.data.redis.password", ""), problems);
        checkPassword("RABBITMQ_PASSWORD", environment.getProperty("spring.rabbitmq.password", ""), problems);
        String origins = environment.getProperty("app.cors.allowed-origins", "");
        for (String origin : origins.split(",")) {
            String value = origin.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (!value.startsWith("https://")) {
                problems.add("CORS_ALLOWED_ORIGINS must use https (found " + value + ")");
            } else if (value.contains("localhost") || value.contains("127.0.0.1")) {
                problems.add("CORS_ALLOWED_ORIGINS must not point at localhost (found " + value + ")");
            }
        }
        if (DEFAULT_BOOTSTRAP_PASSWORD.equals(environment.getProperty("app.auth.bootstrap-admin-password", ""))) {
            problems.add("BOOTSTRAP_ADMIN_PASSWORD still has the example value");
        }
        return problems;
    }

    private static void checkSecret(String name, String value, List<String> problems) {
        if (value.length() < 32) {
            problems.add(name + " must be at least 32 characters");
        } else if (value.toLowerCase(Locale.ROOT).startsWith("change-me")) {
            problems.add(name + " still has the example value");
        }
    }

    private static void checkPassword(String name, String value, List<String> problems) {
        if (value.toLowerCase(Locale.ROOT).startsWith("change-me")) {
            problems.add(name + " still has the example value");
        }
    }
}
