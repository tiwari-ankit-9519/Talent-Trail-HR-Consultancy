package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimelineIT extends IntegrationTestBase {

    private static final String HR_MANAGER_ROLE = "a0000000-0000-0000-0000-000000000006";

    @Test
    void candidateTimelineListsItsEventsAndHidesDownloads() {
        String admin = adminToken();
        String email = "timeline-" + UUID.randomUUID().toString().substring(0, 8) + "@candidates.test";
        Reply created = post("/api/v1/candidates", admin, "{\"fullName\":\"Timeline Candidate\",\"email\":\"" + email
                + "\",\"source\":\"REFERRAL\"}");
        assertEquals(201, created.status(), created.body());
        String id = created.tree().get("id").asString();
        assertEquals(200, post("/api/v1/candidates/" + id + "/consent", admin,
                "{\"status\":\"GRANTED\",\"method\":\"EMAIL\"}").status());

        Reply timeline = get("/api/v1/timeline/CANDIDATE/" + id, admin);
        assertEquals(200, timeline.status(), timeline.body());
        assertTrue(timeline.body().contains("CANDIDATE_CREATED"), timeline.body());
        assertTrue(timeline.body().contains("CANDIDATE_CONSENT_RECORDED"), timeline.body());
        assertFalse(timeline.body().contains("ipAddress"));
        assertFalse(timeline.body().contains("userAgent"));
        assertTrue(timeline.tree().get(0).get("occurredAt").asString().compareTo(
                timeline.tree().get(timeline.tree().size() - 1).get("occurredAt").asString()) >= 0);
    }

    @Test
    void unknownRecordsReturnNotFoundAndMissingPermissionIsForbidden() {
        String admin = adminToken();
        assertEquals(404, get("/api/v1/timeline/PLACEMENT/" + UUID.randomUUID(), admin).status());
        assertEquals(404, get("/api/v1/timeline/INVOICE/" + UUID.randomUUID(), admin).status());
        assertEquals(404, get("/api/v1/timeline/PAYROLL_RUN/" + UUID.randomUUID(), admin).status());

        String email = "tl-hr-" + UUID.randomUUID().toString().substring(0, 8) + "@talenttrail.test";
        Reply user = post("/api/v1/users", admin, "{\"email\":\"" + email + "\",\"fullName\":\"HR Only\","
                + "\"password\":\"Staff-Passw0rd!x\",\"roleIds\":[\"" + HR_MANAGER_ROLE + "\"]}");
        assertEquals(201, user.status(), user.body());
        String staff = loginReady(email, "Staff-Passw0rd!x", "Staff-Passw0rd!y");
        assertEquals(403, get("/api/v1/timeline/PLACEMENT/" + UUID.randomUUID(), staff).status());
        assertEquals(403, get("/api/v1/timeline/PAYROLL_RUN/" + UUID.randomUUID(), staff).status());
    }
}
