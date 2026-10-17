package com.abchr.platform.reminder;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.reminder.ReminderDtos.LogEntry;
import com.abchr.platform.reminder.ReminderDtos.ReminderRequest;
import com.abchr.platform.reminder.ReminderDtos.ReminderView;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    public static final String INVOICE_DUE = "INVOICE_DUE";
    public static final String REPLACEMENT_ENDING = "REPLACEMENT_ENDING";
    public static final String CONSENT_PENDING = "CONSENT_PENDING";
    public static final String DOCUMENT_EXPIRY = "DOCUMENT_EXPIRY";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");

    public record Setting(String code, boolean enabled, List<Integer> offsets, List<String> extraRecipients) {
    }

    private record Info(
            String name,
            String description,
            String audience,
            String reference,
            String offsetsHelp,
            List<String> templates) {
    }

    private static final Map<String, Info> INFO = new LinkedHashMap<>();

    static {
        INFO.put(INVOICE_DUE, new Info("Invoice payment", "Reminds the client about unpaid invoices.", "CLIENT",
                "due date",
                "Days from the due date. Negative is before, 0 is the due date, positive is after.",
                List.of(TemplateCatalog.INVOICE_DUE_SOON, TemplateCatalog.INVOICE_OVERDUE)));
        INFO.put(REPLACEMENT_ENDING, new Info("Replacement period ending",
                "Alerts the recruiter before a free replacement period ends.", "STAFF", "end of the replacement period",
                "Days from the end of the period. Use negative numbers for days before.",
                List.of(TemplateCatalog.REPLACEMENT_ENDING)));
        INFO.put(CONSENT_PENDING, new Info("Consent pending",
                "Alerts the recruiter when a candidate has no consent on record.", "STAFF", "date the candidate was added",
                "Days after the candidate was added.", List.of(TemplateCatalog.CONSENT_PENDING)));
        INFO.put(DOCUMENT_EXPIRY, new Info("Document expiry", "Tells the employee a document is about to expire.",
                "STAFF", "expiry date", "Days from the expiry date. Negative is before, 0 is the expiry date.",
                List.of(TemplateCatalog.DOCUMENT_EXPIRY)));
    }

    private final JdbcTemplate jdbc;
    private final AuditService auditService;

    public ReminderService(JdbcTemplate jdbc, AuditService auditService) {
        this.jdbc = jdbc;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReminderView> list() {
        List<ReminderView> out = new ArrayList<>();
        INFO.keySet().forEach(code -> out.add(view(code, setting(code))));
        return out;
    }

    @Transactional(readOnly = true)
    public Setting setting(String code) {
        List<Setting> rows = jdbc.query(
                "select code, enabled, offsets, extra_recipients from reminder_setting where code = ?",
                (rs, n) -> new Setting(rs.getString("code"), rs.getBoolean("enabled"),
                        parseOffsets(rs.getString("offsets")), parseEmails(rs.getString("extra_recipients"))),
                code);
        if (rows.isEmpty()) {
            throw ApiException.notFound(ErrorCodes.REMINDER_NOT_FOUND, "Reminder not found");
        }
        return rows.get(0);
    }

    @Transactional
    public ReminderView update(String code, ReminderRequest request) {
        Setting before = setting(code);
        TreeSet<Integer> offsets = new TreeSet<>();
        for (Integer offset : request.offsets()) {
            if (offset == null || offset < -365 || offset > 365) {
                throw ApiException.rule(ErrorCodes.REMINDER_INVALID, "Days must be between -365 and 365");
            }
            offsets.add(offset);
        }
        List<String> extras = new ArrayList<>();
        for (String raw : request.extraRecipients()) {
            String email = raw == null ? "" : raw.strip().toLowerCase();
            if (email.isEmpty()) {
                continue;
            }
            if (!EMAIL.matcher(email).matches()) {
                throw ApiException.rule(ErrorCodes.REMINDER_INVALID, "Enter valid email addresses");
            }
            if (!extras.contains(email)) {
                extras.add(email);
            }
        }
        String offsetText = String.join(",", offsets.stream().map(String::valueOf).toList());
        jdbc.update("update reminder_setting set enabled = ?, offsets = ?, extra_recipients = ?, updated_at = now(), "
                + "updated_by = ? where code = ?", request.enabled(), offsetText, String.join(",", extras),
                Actor.current().email(), code);
        auditService.record(AuditActions.REMINDER_UPDATED, AuditActions.ENTITY_REMINDER, code,
                "Changed reminder " + INFO.get(code).name(),
                Changes.create().field("enabled", before.enabled(), request.enabled())
                        .field("offsets", before.offsets().toString(), offsets.toString())
                        .field("extraRecipients", before.extraRecipients().toString(), extras.toString()).toJson());
        return view(code, setting(code));
    }

    @Transactional(readOnly = true)
    public List<LogEntry> log() {
        return jdbc.query(
                "select id, code, offset_days, recipients, subject, sent_at from reminder_log "
                        + "where status = 'SENT' order by sent_at desc limit 100",
                (rs, n) -> new LogEntry(rs.getObject("id", java.util.UUID.class), rs.getString("code"),
                        rs.getInt("offset_days"), rs.getString("recipients"), rs.getString("subject"),
                        rs.getTimestamp("sent_at").toInstant()));
    }

    public static List<String> codes() {
        return List.copyOf(INFO.keySet());
    }

    private ReminderView view(String code, Setting setting) {
        Info info = INFO.get(code);
        return new ReminderView(code, info.name(), info.description(), info.audience(), info.reference(),
                info.offsetsHelp(), info.templates(), setting.enabled(), setting.offsets(),
                setting.extraRecipients());
    }

    static List<Integer> parseOffsets(String text) {
        List<Integer> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String part : text.split(",")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                out.add(Integer.parseInt(trimmed));
            }
        }
        return out;
    }

    static List<String> parseEmails(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                out.add(part.strip());
            }
        }
        return out;
    }
}
