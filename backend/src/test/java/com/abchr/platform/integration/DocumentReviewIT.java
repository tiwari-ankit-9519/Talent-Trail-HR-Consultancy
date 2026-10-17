package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

class DocumentReviewIT extends IntegrationTestBase {

    private static final String RECRUITER_ROLE = "a0000000-0000-0000-0000-000000000003";

    @Autowired
    Environment env;

    private Reply upload(String token, String title) {
        String boundary = "----tt" + UUID.randomUUID();
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"docType\"\r\n\r\nID_PROOF\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"id.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n%PDF-1.4\n%test document\n%%EOF\n\r\n--" + boundary + "--\r\n";
        try {
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("http://localhost:" + env.getProperty("local.server.port")
                            + "/api/v1/profiles/me/documents"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.ISO_8859_1)).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void employeeUploadsAndHrReviews() {
        String admin = adminToken();
        String email = "docs-" + UUID.randomUUID().toString().substring(0, 8) + "@talenttrail.test";
        Reply created = post("/api/v1/users", admin, "{\"email\":\"" + email + "\",\"fullName\":\"Doc Staff\","
                + "\"password\":\"Staff-Passw0rd!x\",\"roleIds\":[\"" + RECRUITER_ROLE + "\"]}");
        assertEquals(201, created.status(), created.body());
        String userId = created.tree().get("id").asString();
        String staff = loginReady(email, "Staff-Passw0rd!x", "Staff-Passw0rd!y");

        Reply first = upload(staff, "Aadhaar");
        assertEquals(201, first.status(), first.body());
        assertEquals("PENDING", first.tree().get("reviewStatus").asString());
        assertTrue(first.tree().get("uploadedBySelf").asBoolean());
        assertTrue(first.tree().get("canDelete").asBoolean());
        assertFalse(first.tree().get("canReview").asBoolean());
        String firstId = first.tree().get("id").asString();

        assertEquals(403, post("/api/v1/profiles/documents/" + firstId + "/review", staff,
                "{\"decision\":\"VERIFIED\"}").status());

        Reply listed = get("/api/v1/profiles/" + userId + "/documents", admin);
        assertEquals(200, listed.status(), listed.body());
        assertTrue(listed.tree().get(0).get("canReview").asBoolean());

        assertEquals(422, post("/api/v1/profiles/documents/" + firstId + "/review", admin,
                "{\"decision\":\"REJECTED\"}").status());
        Reply rejected = post("/api/v1/profiles/documents/" + firstId + "/review", admin,
                "{\"decision\":\"REJECTED\",\"note\":\"Photo is blurry\"}");
        assertEquals(200, rejected.status(), rejected.body());
        assertEquals("REJECTED", rejected.tree().get("reviewStatus").asString());
        assertEquals(204, call("DELETE", "/api/v1/profiles/documents/" + firstId, staff, null).status());

        Reply second = upload(staff, "Aadhaar again");
        assertEquals(201, second.status(), second.body());
        String secondId = second.tree().get("id").asString();
        Reply verified = post("/api/v1/profiles/documents/" + secondId + "/review", admin,
                "{\"decision\":\"VERIFIED\"}");
        assertEquals(200, verified.status(), verified.body());
        assertEquals(422, post("/api/v1/profiles/documents/" + secondId + "/review", admin,
                "{\"decision\":\"REJECTED\",\"note\":\"x\"}").status());
        assertEquals(403, call("DELETE", "/api/v1/profiles/documents/" + secondId, staff, null).status());
        assertEquals(204, call("DELETE", "/api/v1/profiles/documents/" + secondId, admin, null).status());
    }
}
