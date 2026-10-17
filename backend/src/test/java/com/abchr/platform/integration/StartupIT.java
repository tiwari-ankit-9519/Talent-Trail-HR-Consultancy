package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class StartupIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplied() {
        Integer failed = jdbc.queryForObject("select count(*) from flyway_schema_history where success = false",
                Integer.class);
        Integer applied = jdbc.queryForObject("select count(*) from flyway_schema_history where success = true",
                Integer.class);
        assertEquals(0, failed);
        assertTrue(applied >= 21, "expected at least 21 migrations but found " + applied);
    }

    @Test
    void bootstrapAdminExists() {
        Integer count = jdbc.queryForObject("select count(*) from app_user where lower(email) = ?", Integer.class,
                ADMIN_EMAIL);
        assertEquals(1, count);
    }

    @Test
    void healthEndpointIsUp() {
        assertEquals(200, get("/actuator/health", null).status());
    }

    @Test
    void publicStatusEndpointIsUp() {
        assertEquals(200, get("/api/v1/system/status", null).status());
    }
}
