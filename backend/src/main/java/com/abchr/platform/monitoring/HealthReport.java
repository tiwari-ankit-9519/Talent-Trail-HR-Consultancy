package com.abchr.platform.monitoring;

import java.time.Instant;
import java.util.List;

public record HealthReport(
        boolean healthy, Instant checkedAt, Instant startedAt, int liveConnections, List<HealthCheck> checks) {
}
