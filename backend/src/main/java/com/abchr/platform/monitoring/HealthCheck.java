package com.abchr.platform.monitoring;

public record HealthCheck(String key, boolean ok, String detail) {
}
