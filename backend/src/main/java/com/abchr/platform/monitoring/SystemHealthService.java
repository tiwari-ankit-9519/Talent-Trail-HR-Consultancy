package com.abchr.platform.monitoring;

import com.abchr.platform.notification.LiveHub;
import com.abchr.platform.shared.config.AppProperties;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final ObjectProvider<RedisConnectionFactory> redisProvider;
    private final LiveHub liveHub;
    private final AppProperties properties;
    private final int minFreeDiskPercent;
    private final int outboxStuckMinutes;
    private final Instant startedAt;

    public SystemHealthService(
            JdbcTemplate jdbc,
            RabbitTemplate rabbit,
            ObjectProvider<RedisConnectionFactory> redisProvider,
            LiveHub liveHub,
            AppProperties properties,
            @Value("${app.monitoring.min-free-disk-percent:15}") int minFreeDiskPercent,
            @Value("${app.monitoring.outbox-stuck-minutes:10}") int outboxStuckMinutes) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.redisProvider = redisProvider;
        this.liveHub = liveHub;
        this.properties = properties;
        this.minFreeDiskPercent = minFreeDiskPercent;
        this.outboxStuckMinutes = outboxStuckMinutes;
        this.startedAt = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
    }

    public HealthReport run() {
        List<HealthCheck> checks = new ArrayList<>();
        checks.add(database());
        checks.add(rabbitmq());
        checks.add(redis());
        checks.add(outboxBacklog());
        checks.add(outboxFailures());
        checks.add(disk());
        checks.add(memory());
        boolean healthy = checks.stream().allMatch(HealthCheck::ok);
        return new HealthReport(healthy, Instant.now(), startedAt, liveHub.connections(), checks);
    }

    private HealthCheck database() {
        try {
            Integer one = jdbc.queryForObject("select 1", Integer.class);
            return new HealthCheck("database", one != null && one == 1, "PostgreSQL answers queries");
        } catch (RuntimeException ex) {
            return new HealthCheck("database", false, message(ex));
        }
    }

    private HealthCheck rabbitmq() {
        try {
            Boolean open = rabbit.execute(channel -> channel.isOpen());
            return new HealthCheck("rabbitmq", Boolean.TRUE.equals(open), "RabbitMQ connection is open");
        } catch (RuntimeException ex) {
            return new HealthCheck("rabbitmq", false, message(ex));
        }
    }

    private HealthCheck redis() {
        RedisConnectionFactory factory = redisProvider.getIfAvailable();
        if (factory == null) {
            return new HealthCheck("redis", false, "Redis is not configured");
        }
        try (RedisConnection connection = factory.getConnection()) {
            String pong = connection.ping();
            return new HealthCheck("redis", "PONG".equalsIgnoreCase(pong), "Redis answers ping");
        } catch (RuntimeException ex) {
            return new HealthCheck("redis", false, message(ex));
        }
    }

    private HealthCheck outboxBacklog() {
        try {
            Long stuck = jdbc.queryForObject(
                    "select count(*) from outbox_event where status = 'PENDING' and created_at < now() - make_interval(mins => ?)",
                    Long.class, outboxStuckMinutes);
            long count = stuck == null ? 0 : stuck;
            return new HealthCheck("outboxBacklog", count == 0,
                    count == 0 ? "No events waiting" : count + " event(s) waiting for over " + outboxStuckMinutes + " minutes");
        } catch (RuntimeException ex) {
            return new HealthCheck("outboxBacklog", false, message(ex));
        }
    }

    private HealthCheck outboxFailures() {
        try {
            Long failed = jdbc.queryForObject(
                    "select count(*) from outbox_event where status = 'FAILED' and created_at > now() - interval '24 hours'",
                    Long.class);
            long count = failed == null ? 0 : failed;
            return new HealthCheck("outboxFailures", count == 0,
                    count == 0 ? "No failed events in the last 24 hours" : count + " event(s) failed in the last 24 hours");
        } catch (RuntimeException ex) {
            return new HealthCheck("outboxFailures", false, message(ex));
        }
    }

    private HealthCheck disk() {
        try {
            Path path = Path.of(properties.storage().basePath());
            if (!Files.exists(path)) {
                path = path.toAbsolutePath().getRoot();
            }
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            long percent = total == 0 ? 0 : free * 100 / total;
            return new HealthCheck("disk", percent >= minFreeDiskPercent,
                    percent + "% free (" + (free / 1_048_576) + " MB of " + (total / 1_048_576) + " MB)");
        } catch (IOException | RuntimeException ex) {
            return new HealthCheck("disk", false, message(ex));
        }
    }

    private HealthCheck memory() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long percent = max == 0 ? 0 : used * 100 / max;
        return new HealthCheck("memory", percent < 90, percent + "% of heap used (" + (used / 1_048_576) + " MB of "
                + (max / 1_048_576) + " MB)");
    }

    private static String message(Exception ex) {
        String text = ex.getMessage();
        return text == null || text.isBlank() ? ex.getClass().getSimpleName() : text.split("\n")[0];
    }
}
