package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BusinessReportIT extends IntegrationTestBase {

    @Test
    void adminCanOpenEveryBusinessReport() {
        String admin = adminToken();
        for (String path : new String[] {"/api/v1/reports/payroll-register", "/api/v1/reports/leave",
                "/api/v1/reports/receivables", "/api/v1/reports/tax-summary", "/api/v1/reports/commissions"}) {
            Reply reply = get(path, admin);
            assertEquals(200, reply.status(), path + " " + reply.body());
            assertNotNull(reply.tree());
        }
    }

    @Test
    void payrollRegisterWithoutARunIsEmpty() {
        Reply reply = get("/api/v1/reports/payroll-register?month=2020-01", adminToken());
        assertEquals(200, reply.status(), reply.body());
        assertEquals("NONE", reply.tree().get("status").asString());
        assertTrue(reply.tree().get("rows").isEmpty());
    }

    @Test
    void invalidRangeIsRejected() {
        Reply reply = get("/api/v1/reports/tax-summary?from=2026-10-01&to=2026-01-01", adminToken());
        assertEquals(422, reply.status(), reply.body());
    }

    @Test
    void reportsRequireSignIn() {
        assertEquals(401, get("/api/v1/reports/receivables", null).status());
    }
}
