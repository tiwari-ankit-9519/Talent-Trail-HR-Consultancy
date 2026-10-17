package com.abchr.platform.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReminderIT extends IntegrationTestBase {

    @Test
    void adminCanEditPreviewAndResetATemplate() {
        String admin = adminToken();
        Reply list = get("/api/v1/email-templates", admin);
        assertEquals(200, list.status(), list.body());
        assertTrue(list.tree().size() >= 7);

        Reply bad = call("PUT", "/api/v1/email-templates/INVOICE_SEND", admin,
                "{\"subject\":\"Invoice {{nope}}\",\"body\":\"Hello\"}");
        assertEquals(422, bad.status(), bad.body());

        Reply saved = call("PUT", "/api/v1/email-templates/INVOICE_SEND", admin,
                "{\"subject\":\"Bill {{invoiceNumber}}\",\"body\":\"Pay {{amount}} by {{dueDate}}\"}");
        assertEquals(200, saved.status(), saved.body());
        assertTrue(saved.tree().get("customised").asBoolean());

        Reply preview = post("/api/v1/email-templates/INVOICE_SEND/preview", admin,
                "{\"subject\":\"Bill {{invoiceNumber}}\",\"body\":\"Pay {{amount}}\"}");
        assertEquals(200, preview.status(), preview.body());
        assertFalse(preview.tree().get("subject").asString().contains("{{"));

        Reply reset = post("/api/v1/email-templates/INVOICE_SEND/reset", admin, null);
        assertEquals(200, reset.status(), reset.body());
        assertFalse(reset.tree().get("customised").asBoolean());
    }

    @Test
    void remindersStartDisabledAndCanBeConfigured() {
        String admin = adminToken();
        Reply list = get("/api/v1/reminders", admin);
        assertEquals(200, list.status(), list.body());
        assertEquals(4, list.tree().size());
        assertFalse(list.tree().get(0).get("enabled").asBoolean());

        Reply saved = call("PUT", "/api/v1/reminders/INVOICE_DUE", admin,
                "{\"enabled\":true,\"offsets\":[7,-3,0],\"extraRecipients\":[\"Finance@Talenttrail.test\"]}");
        assertEquals(200, saved.status(), saved.body());
        assertEquals("-3", saved.tree().get("offsets").get(0).asString());
        assertEquals("finance@talenttrail.test", saved.tree().get("extraRecipients").get(0).asString());

        Reply invalid = call("PUT", "/api/v1/reminders/INVOICE_DUE", admin,
                "{\"enabled\":true,\"offsets\":[5000],\"extraRecipients\":[]}");
        assertEquals(422, invalid.status(), invalid.body());

        Reply off = call("PUT", "/api/v1/reminders/INVOICE_DUE", admin,
                "{\"enabled\":false,\"offsets\":[-3,0],\"extraRecipients\":[]}");
        assertEquals(200, off.status(), off.body());
    }

    @Test
    void manualRunWorksWithoutMailConfigured() {
        Reply run = post("/api/v1/reminders/run", adminToken(), null);
        assertEquals(200, run.status(), run.body());
    }

    @Test
    void remindersRequireSignInAndPermission() {
        assertEquals(401, get("/api/v1/reminders", null).status());
    }
}
