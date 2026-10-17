package com.abchr.platform.reminder;

import com.abchr.platform.reminder.ReminderDtos.Rendered;
import com.abchr.platform.reminder.ReminderDtos.RunResult;
import com.abchr.platform.reminder.ReminderService.Setting;
import com.abchr.platform.settings.CompanyService;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.mail.MailService;
import java.sql.Date;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReminderJob {

    private static final Logger LOG = LogManager.getLogger(ReminderJob.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final int CATCH_UP_DAYS = 2;
    private static final int MAX_PER_RUN = 300;

    private record Target(UUID entityId, LocalDate reference, List<String> to, Map<String, String> values) {
    }

    private final JdbcTemplate jdbc;
    private final ReminderService reminderService;
    private final EmailTemplateService templateService;
    private final MailService mailService;
    private final CompanyService companyService;
    private final ZoneId zone;
    private final AtomicBoolean running = new AtomicBoolean();

    public ReminderJob(
            JdbcTemplate jdbc,
            ReminderService reminderService,
            EmailTemplateService templateService,
            MailService mailService,
            CompanyService companyService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.jdbc = jdbc;
        this.reminderService = reminderService;
        this.templateService = templateService;
        this.mailService = mailService;
        this.companyService = companyService;
        this.zone = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}", zone = "${app.timezone:Asia/Kolkata}")
    public void scheduled() {
        run();
    }

    public RunResult run() {
        Map<String, Integer> sent = new LinkedHashMap<>();
        if (!running.compareAndSet(false, true)) {
            return new RunResult(sent, mailService.isConfigured());
        }
        try {
            if (!mailService.isConfigured()) {
                LOG.warn("Reminders skipped: email is not configured on the server");
                return new RunResult(sent, false);
            }
            LocalDate today = LocalDate.now(zone);
            for (String code : ReminderService.codes()) {
                Setting setting = reminderService.setting(code);
                if (!setting.enabled() || setting.offsets().isEmpty()) {
                    continue;
                }
                try {
                    sent.put(code, process(code, setting, today));
                } catch (RuntimeException ex) {
                    LOG.error("Reminder {} failed", code, ex);
                }
            }
            LOG.info("Reminders finished: {}", sent);
            return new RunResult(sent, true);
        } finally {
            running.set(false);
        }
    }

    private int process(String code, Setting setting, LocalDate today) {
        int sent = 0;
        String replyTo = companyService.load().getEmail();
        for (Target target : targets(code, setting, today)) {
            long delta = ChronoUnit.DAYS.between(target.reference(), today);
            List<Integer> descending = new ArrayList<>(setting.offsets());
            descending.sort(java.util.Comparator.reverseOrder());
            for (int offset : descending) {
                if (delta < offset || delta - offset > CATCH_UP_DAYS) {
                    continue;
                }
                if (sent >= MAX_PER_RUN) {
                    return sent;
                }
                if (deliver(code, setting, target, offset, delta, replyTo)) {
                    sent++;
                }
                break;
            }
        }
        return sent;
    }

    private boolean deliver(String code, Setting setting, Target target, int offset, long delta, String replyTo) {
        UUID logId = UUID.randomUUID();
        int claimed = jdbc.update(
                "insert into reminder_log (id, code, entity_id, offset_days, recipients, subject, status) "
                        + "values (?, ?, ?, ?, '', '', 'SENDING') on conflict (code, entity_id, offset_days) do nothing",
                logId, code, target.entityId(), offset);
        if (claimed == 0) {
            return false;
        }
        Set<String> recipients = new LinkedHashSet<>(target.to());
        recipients.addAll(setting.extraRecipients());
        if (recipients.isEmpty()) {
            jdbc.update("delete from reminder_log where id = ?", logId);
            return false;
        }
        Map<String, String> values = new LinkedHashMap<>(target.values());
        String template = templateFor(code, delta);
        if (TemplateCatalog.INVOICE_DUE_SOON.equals(template)) {
            values.put("daysLeft", String.valueOf(Math.max(0, -delta)));
        }
        if (TemplateCatalog.INVOICE_OVERDUE.equals(template)) {
            values.put("daysOverdue", String.valueOf(delta));
        }
        if (REPLACEMENT.equals(code) || DOCUMENT.equals(code)) {
            values.put("daysLeft", String.valueOf(Math.max(0, -delta)));
        }
        if (CONSENT.equals(code)) {
            values.put("daysWaiting", String.valueOf(delta));
        }
        Rendered rendered = templateService.render(template, values);
        MailService.Result result = mailService.send(new ArrayList<>(recipients), rendered.subject(), rendered.body(),
                null, replyTo);
        if (result.status() == EmailStatus.SENT) {
            jdbc.update("update reminder_log set status = 'SENT', recipients = ?, subject = ?, sent_at = now() "
                    + "where id = ?", abbreviate(String.join(", ", recipients), 790),
                    abbreviate(rendered.subject(), 290), logId);
            return true;
        }
        LOG.warn("Reminder {} for {} not sent: {} {}", code, target.entityId(), result.status(), result.error());
        jdbc.update("delete from reminder_log where id = ?", logId);
        return false;
    }

    private static final String REPLACEMENT = ReminderService.REPLACEMENT_ENDING;
    private static final String DOCUMENT = ReminderService.DOCUMENT_EXPIRY;
    private static final String CONSENT = ReminderService.CONSENT_PENDING;

    private static String templateFor(String code, long delta) {
        return switch (code) {
            case ReminderService.INVOICE_DUE ->
                delta > 0 ? TemplateCatalog.INVOICE_OVERDUE : TemplateCatalog.INVOICE_DUE_SOON;
            case REPLACEMENT -> TemplateCatalog.REPLACEMENT_ENDING;
            case CONSENT -> TemplateCatalog.CONSENT_PENDING;
            default -> TemplateCatalog.DOCUMENT_EXPIRY;
        };
    }

    private List<Target> targets(String code, Setting setting, LocalDate today) {
        int min = setting.offsets().stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = setting.offsets().stream().mapToInt(Integer::intValue).max().orElse(0) + CATCH_UP_DAYS;
        LocalDate from = today.minusDays(max);
        LocalDate to = today.minusDays(min);
        return switch (code) {
            case ReminderService.INVOICE_DUE -> invoiceTargets(from, to);
            case REPLACEMENT -> replacementTargets(from, to);
            case CONSENT -> consentTargets(from, to);
            default -> documentTargets(from, to);
        };
    }

    private List<Target> invoiceTargets(LocalDate from, LocalDate to) {
        String company = templateService.companyName();
        return jdbc.query(
                "select i.id, i.number, i.due_date, i.candidate_name, i.total_amount - i.settled_amount as outstanding, "
                        + "cl.name as client, i.client_id from invoice i join client cl on cl.id = i.client_id "
                        + "where i.status in ('ISSUED', 'PARTIALLY_PAID') and i.due_date between ? and ? "
                        + "order by i.due_date",
                (rs, n) -> {
                    UUID clientId = rs.getObject("client_id", UUID.class);
                    Map<String, String> values = new LinkedHashMap<>();
                    values.put("company", company);
                    values.put("client", rs.getString("client"));
                    values.put("invoiceNumber", rs.getString("number"));
                    values.put("candidate", rs.getString("candidate_name"));
                    values.put("outstanding", money(rs.getBigDecimal("outstanding")));
                    LocalDate due = rs.getDate("due_date").toLocalDate();
                    values.put("dueDate", DAY.format(due));
                    return new Target(rs.getObject("id", UUID.class), due, clientEmails(clientId), values);
                },
                Date.valueOf(from), Date.valueOf(to));
    }

    private List<String> clientEmails(UUID clientId) {
        List<String> primary = jdbc.queryForList(
                "select email from client_contact where client_id = ? and primary_contact order by created_at",
                String.class, clientId);
        if (!primary.isEmpty()) {
            return primary;
        }
        return jdbc.queryForList("select email from client_contact where client_id = ? order by created_at limit 2",
                String.class, clientId);
    }

    private List<Target> replacementTargets(LocalDate from, LocalDate to) {
        return jdbc.query(
                "select c.id, c.payable_after, cand.full_name as candidate, cl.name as client, j.title, "
                        + "coalesce(u.full_name, c.recruiter_name, 'there') as recruiter, "
                        + "coalesce(u.email, c.recruiter_email) as email from commission c "
                        + "join placement p on p.id = c.placement_id join candidate cand on cand.id = p.candidate_id "
                        + "join client cl on cl.id = p.client_id join job j on j.id = p.job_id "
                        + "left join app_user u on u.id = c.recruiter_user_id "
                        + "where c.status = 'PENDING' and p.status = 'JOINED' and c.payable_after between ? and ? "
                        + "and not exists (select 1 from replacement r where r.placement_id = p.id)",
                (rs, n) -> {
                    LocalDate end = rs.getDate("payable_after").toLocalDate();
                    Map<String, String> values = new LinkedHashMap<>();
                    values.put("recruiter", rs.getString("recruiter"));
                    values.put("candidate", rs.getString("candidate"));
                    values.put("client", rs.getString("client"));
                    values.put("jobTitle", rs.getString("title"));
                    values.put("endDate", DAY.format(end));
                    String email = rs.getString("email");
                    return new Target(rs.getObject("id", UUID.class), end,
                            email == null || email.isBlank() ? List.of() : List.of(email), values);
                },
                Date.valueOf(from), Date.valueOf(to));
    }

    private List<Target> consentTargets(LocalDate from, LocalDate to) {
        return jdbc.query(
                "select id, full_name, email, created_by, (created_at at time zone ?)::date as added from candidate "
                        + "where consent_status = 'NOT_ASKED' and status in ('NEW', 'SCREENING', 'AVAILABLE') "
                        + "and (created_at at time zone ?)::date between ? and ?",
                (rs, n) -> {
                    String owner = rs.getString("created_by");
                    Map<String, String> values = new LinkedHashMap<>();
                    values.put("recruiter", owner == null ? "" : owner);
                    values.put("candidate", rs.getString("full_name"));
                    values.put("candidateEmail", rs.getString("email"));
                    return new Target(rs.getObject("id", UUID.class), rs.getDate("added").toLocalDate(),
                            owner != null && owner.contains("@") ? List.of(owner) : List.of(), values);
                },
                zone.getId(), zone.getId(), Date.valueOf(from), Date.valueOf(to));
    }

    private List<Target> documentTargets(LocalDate from, LocalDate to) {
        return jdbc.query(
                "select d.id, d.title, d.expires_on, u.full_name, u.email from employee_document d "
                        + "join app_user u on u.id = d.user_id where u.status = 'ACTIVE' and d.expires_on between ? and ?",
                (rs, n) -> {
                    LocalDate expiry = rs.getDate("expires_on").toLocalDate();
                    Map<String, String> values = new LinkedHashMap<>();
                    values.put("employee", rs.getString("full_name"));
                    values.put("document", rs.getString("title"));
                    values.put("expiryDate", DAY.format(expiry));
                    return new Target(rs.getObject("id", UUID.class), expiry, List.of(rs.getString("email")), values);
                },
                Date.valueOf(from), Date.valueOf(to));
    }

    private static String money(java.math.BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.of("en", "IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "Rs. " + format.format(value);
    }

    private static String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
