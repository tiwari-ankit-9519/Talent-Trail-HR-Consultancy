package com.abchr.platform.reminder;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.reminder.ReminderDtos.Rendered;
import com.abchr.platform.reminder.ReminderDtos.TemplateRequest;
import com.abchr.platform.reminder.ReminderDtos.TemplateView;
import com.abchr.platform.reminder.ReminderDtos.TestResult;
import com.abchr.platform.reminder.ReminderDtos.VariableView;
import com.abchr.platform.reminder.TemplateCatalog.Definition;
import com.abchr.platform.settings.CompanyService;
import com.abchr.platform.shared.mail.MailService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9]*)\\s*}}");

    private record Stored(String subject, String body, java.time.Instant updatedAt) {
    }

    private final JdbcTemplate jdbc;
    private final AuditService auditService;
    private final MailService mailService;
    private final CompanyService companyService;

    public EmailTemplateService(
            JdbcTemplate jdbc, AuditService auditService, MailService mailService, CompanyService companyService) {
        this.jdbc = jdbc;
        this.auditService = auditService;
        this.mailService = mailService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> list() {
        Map<String, Stored> stored = loadStored();
        List<TemplateView> out = new ArrayList<>();
        for (Definition definition : TemplateCatalog.all()) {
            out.add(view(definition, stored.get(definition.code())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public TemplateView get(String code) {
        Definition definition = definition(code);
        return view(definition, loadStored().get(code));
    }

    @Transactional
    public TemplateView update(String code, TemplateRequest request) {
        Definition definition = definition(code);
        String subject = request.subject().strip();
        String body = request.body().strip();
        validate(definition, subject, body);
        TemplateView before = view(definition, loadStored().get(code));
        String actor = Actor.current().email();
        jdbc.update(
                "insert into email_template (code, subject, body, created_by, updated_by) values (?, ?, ?, ?, ?) "
                        + "on conflict (code) do update set subject = excluded.subject, body = excluded.body, "
                        + "version = email_template.version + 1, updated_at = now(), updated_by = excluded.updated_by",
                code, subject, body, actor, actor);
        auditService.record(AuditActions.EMAIL_TEMPLATE_UPDATED, AuditActions.ENTITY_EMAIL_TEMPLATE, code,
                "Edited email template " + definition.name(),
                Changes.create().field("subject", before.subject(), subject).field("body", before.body(), body)
                        .toJson());
        return get(code);
    }

    @Transactional
    public TemplateView reset(String code) {
        Definition definition = definition(code);
        jdbc.update("delete from email_template where code = ?", code);
        auditService.record(AuditActions.EMAIL_TEMPLATE_RESET, AuditActions.ENTITY_EMAIL_TEMPLATE, code,
                "Reset email template " + definition.name() + " to default", null);
        return get(code);
    }

    @Transactional(readOnly = true)
    public Rendered preview(String code, TemplateRequest request) {
        Definition definition = definition(code);
        String subject = request.subject().strip();
        String body = request.body().strip();
        validate(definition, subject, body);
        return new Rendered(fill(subject, definition.samples(), true), fill(body, definition.samples(), false));
    }

    @Transactional(readOnly = true)
    public Rendered render(String code, Map<String, String> values) {
        Definition definition = definition(code);
        Stored stored = loadStored().get(code);
        String subject = stored == null ? definition.subject() : stored.subject();
        String body = stored == null ? definition.body() : stored.body();
        Map<String, String> merged = new HashMap<>(values);
        merged.putIfAbsent("company", companyName());
        return new Rendered(fill(subject, merged, true), fill(body, merged, false));
    }

    public TestResult sendTest(String code) {
        Definition definition = definition(code);
        String to = Actor.current().email();
        Rendered rendered = render(code, definition.samples());
        MailService.Result result = mailService.send(List.of(to), "[Test] " + rendered.subject(), rendered.body(),
                null, null);
        auditService.record(AuditActions.EMAIL_TEMPLATE_TESTED, AuditActions.ENTITY_EMAIL_TEMPLATE, code,
                "Sent a test of " + definition.name() + " to " + to + ": " + result.status(), null);
        return new TestResult(result.status().name(), result.error(), to);
    }

    String companyName() {
        String name = companyService.load().getLegalName();
        return name == null || name.isBlank() ? "Talent Trail HR Solutions" : name;
    }

    private Definition definition(String code) {
        Definition definition = TemplateCatalog.find(code);
        if (definition == null) {
            throw ApiException.notFound(ErrorCodes.EMAIL_TEMPLATE_NOT_FOUND, "Email template not found");
        }
        return definition;
    }

    private void validate(Definition definition, String subject, String body) {
        if (subject.isBlank() || body.isBlank()) {
            throw ApiException.rule(ErrorCodes.EMAIL_TEMPLATE_INVALID, "The subject and body cannot be empty");
        }
        if (subject.indexOf('\n') >= 0 || subject.indexOf('\r') >= 0) {
            throw ApiException.rule(ErrorCodes.EMAIL_TEMPLATE_INVALID, "The subject must be a single line");
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (String text : List.of(subject, body)) {
            Matcher matcher = PLACEHOLDER.matcher(text);
            while (matcher.find()) {
                if (!definition.allows(matcher.group(1))) {
                    unknown.add(matcher.group(1));
                }
            }
        }
        if (!unknown.isEmpty()) {
            throw ApiException.rule(ErrorCodes.EMAIL_TEMPLATE_INVALID,
                    "Unknown placeholders: " + String.join(", ", unknown));
        }
    }

    static String fill(String text, Map<String, String> values, boolean singleLine) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(out);
        String result = out.toString();
        return singleLine ? result.replaceAll("[\\r\\n]+", " ").strip() : result;
    }

    private Map<String, Stored> loadStored() {
        Map<String, Stored> out = new HashMap<>();
        jdbc.query("select code, subject, body, updated_at from email_template", rs -> {
            Timestamp at = rs.getTimestamp("updated_at");
            out.put(rs.getString("code"),
                    new Stored(rs.getString("subject"), rs.getString("body"), at == null ? null : at.toInstant()));
        });
        return out;
    }

    private TemplateView view(Definition definition, Stored stored) {
        List<VariableView> variables = definition.variables().stream()
                .map(v -> new VariableView(v.name(), v.description(), v.sample())).toList();
        return new TemplateView(definition.code(), definition.name(), definition.description(),
                definition.audience(), variables, stored == null ? definition.subject() : stored.subject(),
                stored == null ? definition.body() : stored.body(), definition.subject(), definition.body(),
                stored != null, stored == null ? null : stored.updatedAt());
    }
}
