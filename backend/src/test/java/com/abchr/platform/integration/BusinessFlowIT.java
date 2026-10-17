package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BusinessFlowIT extends IntegrationTestBase {

    @Test
    void systemHealthReportsAllChecks() {
        Reply reply = get("/api/v1/system/health", adminToken());
        assertEquals(200, reply.status(), reply.body());
        assertTrue(reply.body().contains("database"));
        assertTrue(reply.body().contains("rabbitmq"));
    }

    @Test
    void leaveTypesAreSeeded() {
        Reply reply = get("/api/v1/attendance/leave-types", adminToken());
        assertEquals(200, reply.status(), reply.body());
        assertTrue(reply.tree().size() >= 4, reply.body());
        assertTrue(reply.body().contains("Loss of pay"));
    }

    @Test
    void salaryPreviewMatchesCtcRules() {
        Reply reply = post("/api/v1/payroll/structures/preview", adminToken(),
                "{\"ctcAnnual\":1800000,\"variablePercent\":10,\"pfApplicable\":true}");
        assertEquals(200, reply.status(), reply.body());
        double gross = reply.tree().get("gross").asDouble();
        assertEquals(130071, gross, 1.0);
        assertEquals(1800, reply.tree().get("employerPf").asDouble(), 0.01);
    }

    @Test
    void payrollListsLoadForAdmin() {
        String token = adminToken();
        assertEquals(200, get("/api/v1/payroll/settings", token).status());
        assertEquals(200, get("/api/v1/payroll/employees", token).status());
        assertEquals(200, get("/api/v1/payroll/runs", token).status());
    }

    @Test
    void dashboardAndNotificationsLoad() {
        String token = adminToken();
        assertEquals(200, get("/api/v1/dashboard", token).status());
        assertEquals(200, get("/api/v1/notifications", token).status());
    }

    @Test
    void myLeaveBalancesLoad() {
        Reply reply = get("/api/v1/attendance/leave/balances", adminToken());
        assertEquals(200, reply.status(), reply.body());
    }
}
