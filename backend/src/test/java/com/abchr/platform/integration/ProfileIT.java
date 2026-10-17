package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileIT extends IntegrationTestBase {

    private static final String RECRUITER_ROLE = "a0000000-0000-0000-0000-000000000003";

    private String newUser(String admin, String password) {
        String email = "staff-" + UUID.randomUUID().toString().substring(0, 8) + "@talenttrail.test";
        Reply created = post("/api/v1/users", admin, "{\"email\":\"" + email + "\",\"fullName\":\"Test Staff\","
                + "\"password\":\"" + password + "\",\"roleIds\":[\"" + RECRUITER_ROLE + "\"]}");
        assertEquals(201, created.status(), created.body());
        return created.tree().get("id").asString() + "|" + email;
    }

    @Test
    void hrCanFillProfileAndBankDetailsAreStoredEncrypted() {
        String admin = adminToken();
        String id = newUser(admin, "Staff-Passw0rd!x").split("\\|")[0];

        Reply empty = get("/api/v1/profiles/" + id, admin);
        assertEquals(200, empty.status(), empty.body());

        Reply updated = call("PUT", "/api/v1/profiles/" + id, admin,
                "{\"employeeCode\":\"TT-" + id.substring(0, 6) + "\",\"employmentType\":\"FULL_TIME\","
                        + "\"designation\":\"Recruiter\",\"department\":\"Recruitment\",\"joiningDate\":\"2026-04-01\","
                        + "\"phone\":\"9876543210\",\"pincode\":\"226001\"}");
        assertEquals(200, updated.status(), updated.body());
        assertEquals("Recruiter", updated.tree().get("designation").asString());

        Reply sensitive = call("PUT", "/api/v1/profiles/" + id + "/sensitive", admin,
                "{\"bankAccountHolder\":\"Test Staff\",\"bankName\":\"HDFC\",\"bankAccountNumber\":\"123456789012\","
                        + "\"bankIfsc\":\"hdfc0001234\",\"pan\":\"abcde1234f\",\"uan\":\"123456789012\","
                        + "\"version\":" + updated.tree().get("version").asLong() + "}");
        assertEquals(200, sensitive.status(), sensitive.body());
        assertEquals("123456789012", sensitive.tree().get("sensitive").get("bankAccountNumber").asString());
        assertEquals("ABCDE1234F", sensitive.tree().get("sensitive").get("pan").asString());
        assertEquals("HDFC0001234", sensitive.tree().get("sensitive").get("bankIfsc").asString());
        assertFalse(sensitive.tree().get("sensitive").get("masked").asBoolean());
    }

    @Test
    void employeeSeesOwnProfileMaskedAndEditsContactOnly() {
        String admin = adminToken();
        String password = "Staff-Passw0rd!x";
        String[] created = newUser(admin, password).split("\\|");
        String id = created[0];

        Reply seeded = call("PUT", "/api/v1/profiles/" + id + "/sensitive", admin,
                "{\"bankAccountNumber\":\"123456789012\",\"bankIfsc\":\"HDFC0001234\"}");
        assertEquals(200, seeded.status(), seeded.body());

        String token = loginReady(created[1], password, "Staff-Passw0rd!y");
        Reply mine = get("/api/v1/profiles/me", token);
        assertEquals(200, mine.status(), mine.body());
        assertTrue(mine.tree().get("sensitive").get("masked").asBoolean());
        assertEquals("********9012", mine.tree().get("sensitive").get("bankAccountNumber").asString());
        assertFalse(mine.tree().get("canEdit").asBoolean());
        assertTrue(mine.tree().get("canEditContact").asBoolean());

        Reply contact = call("PUT", "/api/v1/profiles/me/contact", token,
                "{\"phone\":\"9123456780\",\"city\":\"Lucknow\",\"version\":" + mine.tree().get("version").asLong()
                        + "}");
        assertEquals(200, contact.status(), contact.body());
        assertEquals("Lucknow", contact.tree().get("city").asString());

        assertEquals(403, call("PUT", "/api/v1/profiles/" + id, token,
                "{\"employmentType\":\"FULL_TIME\"}").status());
        assertEquals(403, call("PUT", "/api/v1/profiles/" + id + "/sensitive", token,
                "{\"pan\":\"ABCDE1234F\"}").status());
        assertEquals(403, get("/api/v1/profiles", token).status());
        assertEquals(403, get("/api/v1/profiles/" + UUID.randomUUID(), token).status());
    }

    @Test
    void invalidIdentifiersAreRejected() {
        String admin = adminToken();
        String id = newUser(admin, "Staff-Passw0rd!x").split("\\|")[0];
        assertEquals(400, call("PUT", "/api/v1/profiles/" + id + "/sensitive", admin,
                "{\"pan\":\"not-a-pan\"}").status());
        assertEquals(400, call("PUT", "/api/v1/profiles/" + id + "/sensitive", admin,
                "{\"bankIfsc\":\"123\"}").status());
    }

    @Test
    void managerCannotBeTheEmployeeThemself() {
        String admin = adminToken();
        String id = newUser(admin, "Staff-Passw0rd!x").split("\\|")[0];
        Reply reply = call("PUT", "/api/v1/profiles/" + id, admin,
                "{\"employmentType\":\"FULL_TIME\",\"managerUserId\":\"" + id + "\"}");
        assertEquals(422, reply.status(), reply.body());
    }

    @Test
    void listIncludesTheNewEmployee() {
        String admin = adminToken();
        String id = newUser(admin, "Staff-Passw0rd!x").split("\\|")[0];
        Reply list = get("/api/v1/profiles?q=Test%20Staff", admin);
        assertEquals(200, list.status(), list.body());
        assertTrue(list.body().contains(id));
        assertNotNull(list.tree());
    }
}
