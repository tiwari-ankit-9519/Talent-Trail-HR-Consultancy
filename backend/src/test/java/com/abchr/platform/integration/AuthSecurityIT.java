package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthSecurityIT extends IntegrationTestBase {

    private static final String RECRUITER_ROLE = "a0000000-0000-0000-0000-000000000003";

    @Test
    void protectedEndpointsRejectAnonymousCallers() {
        assertEquals(401, get("/api/v1/users", null).status());
        assertEquals(401, get("/api/v1/payroll/runs", null).status());
        assertEquals(401, get("/api/v1/system/health", null).status());
        assertEquals(401, get("/api/v1/auth/me", null).status());
    }

    @Test
    void browserSignInThroughAProxyIsNotBlockedByCors() {
        Reply reply = call("POST", "/api/v1/auth/login", null,
                "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong-password-value\"}",
                java.util.Map.of("Origin", "https://app.example.test", "X-Forwarded-Host", "app.example.test",
                        "X-Forwarded-Proto", "https"));
        assertEquals(401, reply.status(), reply.body());
    }

    @Test
    void foreignOriginIsBlockedByCors() {
        Reply reply = call("POST", "/api/v1/auth/login", null,
                "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong-password-value\"}",
                java.util.Map.of("Origin", "https://evil.example.test"));
        assertEquals(403, reply.status());
    }

    @Test
    void garbageTokenIsRejected() {
        assertEquals(401, get("/api/v1/auth/me", "not-a-token").status());
    }

    @Test
    void wrongPasswordIsRejectedWithGenericError() {
        Reply reply = post("/api/v1/auth/login", null,
                "{\"email\":\"" + ADMIN_EMAIL + "x\",\"password\":\"Wrong-Passw0rd!\"}");
        assertEquals(401, reply.status());
    }

    @Test
    void adminCanSignInAndSeesOwnProfile() {
        String token = adminToken();
        Reply me = get("/api/v1/auth/me", token);
        assertEquals(200, me.status());
        assertEquals(ADMIN_EMAIL, me.tree().get("email").asString());
        assertTrue(me.tree().get("permissions").size() > 10);
    }

    @Test
    void refreshWithoutGuardHeaderIsForbidden() {
        assertEquals(403, post("/api/v1/auth/refresh", null, null).status());
    }

    @Test
    void recruiterCannotReachAdminOnlyAreas() {
        String admin = adminToken();
        String email = "recruiter-" + UUID.randomUUID().toString().substring(0, 8) + "@talenttrail.test";
        String password = "Rec-Passw0rd!x";
        Reply created = post("/api/v1/users", admin, "{\"email\":\"" + email + "\",\"fullName\":\"Test Recruiter\","
                + "\"password\":\"" + password + "\",\"roleIds\":[\"" + RECRUITER_ROLE + "\"]}");
        assertEquals(201, created.status(), created.body());

        String token = loginReady(email, password, "Rec-Passw0rd!y");

        assertEquals(403, get("/api/v1/payroll/runs", token).status());
        assertEquals(403, get("/api/v1/system/health", token).status());
        assertEquals(403, get("/api/v1/users", token).status());
        assertEquals(200, get("/api/v1/payroll/payslips/mine", token).status());
        assertFalse(get("/api/v1/auth/me", token).tree().get("permissions").isEmpty());
    }
}
