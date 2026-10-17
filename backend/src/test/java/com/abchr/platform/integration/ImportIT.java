package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportIT extends IntegrationTestBase {

    private static String rows(String... items) {
        return "{\"rows\":[" + String.join(",", items) + "]}";
    }

    @Test
    void candidatesPreviewThenCommitSkipsDuplicatesAndInvalidRows() {
        String admin = adminToken();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String good = "{\"fullName\":\"Asha Verma\",\"email\":\"asha-" + tag + "@example.com\",\"source\":\"referral\","
                + "\"totalExperienceYears\":\"4.5\"}";
        String bad = "{\"fullName\":\"No Email\",\"email\":\"not-an-email\"}";
        String twin = "{\"fullName\":\"Asha Again\",\"email\":\"ASHA-" + tag + "@example.com\"}";
        String body = rows(good, bad, twin);

        Reply preview = post("/api/v1/imports/candidates/preview", admin, body);
        assertEquals(200, preview.status(), preview.body());
        assertFalse(preview.tree().get("committed").asBoolean());
        assertEquals(1, preview.tree().get("valid").asInt());
        assertEquals(1, preview.tree().get("invalid").asInt());
        assertEquals(1, preview.tree().get("duplicates").asInt());

        Reply commit = post("/api/v1/imports/candidates/commit", admin, body);
        assertEquals(200, commit.status(), commit.body());
        assertEquals(1, commit.tree().get("imported").asInt());

        Reply again = post("/api/v1/imports/candidates/commit", admin, rows(good));
        assertEquals(0, again.tree().get("imported").asInt(), again.body());
        assertEquals(1, again.tree().get("duplicates").asInt());

        Reply listed = get("/api/v1/candidates?q=asha-" + tag, admin);
        assertEquals(200, listed.status(), listed.body());
        assertTrue(listed.body().contains("NOT_ASKED"));
    }

    @Test
    void clientRowsWithTheSameNameBecomeOneClientWithContacts() {
        String admin = adminToken();
        String name = "Import Client " + UUID.randomUUID().toString().substring(0, 8);
        String first = "{\"name\":\"" + name + "\",\"city\":\"Lucknow\",\"contactName\":\"Ravi\","
                + "\"contactEmail\":\"ravi@example.com\",\"contactPrimary\":\"yes\"}";
        String second = "{\"name\":\"" + name + "\",\"contactName\":\"Meera\",\"contactEmail\":\"meera@example.com\"}";
        Reply commit = post("/api/v1/imports/clients/commit", admin, rows(first, second));
        assertEquals(200, commit.status(), commit.body());
        assertEquals(2, commit.tree().get("imported").asInt(), commit.body());

        Reply again = post("/api/v1/imports/clients/preview", admin, rows(first));
        assertEquals(1, again.tree().get("duplicates").asInt(), again.body());
    }

    @Test
    void employeesGetGeneratedPasswordsAndUnknownRolesAreRejected() {
        String admin = adminToken();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String ok = "{\"fullName\":\"New Hire\",\"email\":\"hire-" + tag + "@talenttrail.test\",\"role\":\"Recruiter\","
                + "\"employeeCode\":\"IMP-" + tag + "\",\"joiningDate\":\"2026-10-01\"}";
        String badRole = "{\"fullName\":\"Odd Role\",\"email\":\"odd-" + tag + "@talenttrail.test\",\"role\":\"Wizard\"}";
        Reply commit = post("/api/v1/imports/employees/commit", admin, rows(ok, badRole));
        assertEquals(200, commit.status(), commit.body());
        assertEquals(1, commit.tree().get("imported").asInt(), commit.body());
        assertEquals(1, commit.tree().get("invalid").asInt());
        assertEquals(1, commit.tree().get("credentials").size());
        assertEquals(16, commit.tree().get("credentials").get(0).get("password").asString().length());
    }

    @Test
    void importNeedsSignInAndAKnownType() {
        assertEquals(401, post("/api/v1/imports/candidates/preview", null, rows("{}")).status());
        assertEquals(404, post("/api/v1/imports/widgets/preview", adminToken(), rows("{}")).status());
    }
}
