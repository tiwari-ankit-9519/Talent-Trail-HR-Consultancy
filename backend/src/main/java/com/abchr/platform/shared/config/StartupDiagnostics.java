package com.abchr.platform.shared.config;

import java.net.URI;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class StartupDiagnostics implements ApplicationRunner {

    private static final Logger LOG = LogManager.getLogger(StartupDiagnostics.class);

    private final Environment environment;
    private final AppProperties properties;
    private final String datasourceUrl;
    private final String environmentName;

    public StartupDiagnostics(
            Environment environment,
            AppProperties properties,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${APP_ENV:local}") String environmentName) {
        this.environment = environment;
        this.properties = properties;
        this.datasourceUrl = datasourceUrl;
        this.environmentName = environmentName;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Startup configuration: env={} java={} profiles={} port={} logDir={} storage={} corsOrigins={}"
                        + " accessTokenMinutes={} refreshTokenDays={} maxFailedLogins={} lockMinutes={}"
                        + " cookieSecure={} outboxExchange={} outboxPollMs={} applyRateLimitPerHour={}"
                        + " uploadMaxFile={} database={}",
                environmentName,
                System.getProperty("java.version"),
                Arrays.toString(environment.getActiveProfiles()),
                environment.getProperty("server.port", "8080"),
                environment.getProperty("LOG_DIR", "logs"),
                properties.storage().basePath(),
                properties.cors().allowedOrigins(),
                properties.security().jwtAccessTtlMinutes(),
                properties.security().jwtRefreshTtlDays(),
                properties.auth().maxFailedAttempts(),
                properties.auth().lockMinutes(),
                properties.auth().cookieSecure(),
                properties.outbox().exchange(),
                properties.outbox().pollIntervalMs(),
                environment.getProperty("app.apply.rate-limit-per-hour", "10"),
                environment.getProperty("spring.servlet.multipart.max-file-size", "?"),
                safeDatabase(datasourceUrl));
    }

    private static String safeDatabase(String url) {
        if (url == null || url.isBlank()) {
            return "unset";
        }
        try {
            URI uri = URI.create(url.startsWith("jdbc:") ? url.substring(5) : url);
            return uri.getHost() + ":" + uri.getPort() + uri.getPath();
        } catch (IllegalArgumentException ex) {
            return "unparseable";
        }
    }
}
