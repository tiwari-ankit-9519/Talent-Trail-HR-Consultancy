package com.abchr.platform.reminder;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.reminder.ReminderDtos.LogEntry;
import com.abchr.platform.reminder.ReminderDtos.ReminderRequest;
import com.abchr.platform.reminder.ReminderDtos.ReminderView;
import com.abchr.platform.reminder.ReminderDtos.Rendered;
import com.abchr.platform.reminder.ReminderDtos.RunResult;
import com.abchr.platform.reminder.ReminderDtos.TemplateRequest;
import com.abchr.platform.reminder.ReminderDtos.TemplateView;
import com.abchr.platform.reminder.ReminderDtos.TestResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('EMAIL_TEMPLATES_MANAGE')")
public class ReminderController {

    private final EmailTemplateService templates;
    private final ReminderService reminders;
    private final ReminderJob job;
    private final AuditService auditService;

    public ReminderController(
            EmailTemplateService templates, ReminderService reminders, ReminderJob job, AuditService auditService) {
        this.templates = templates;
        this.reminders = reminders;
        this.job = job;
        this.auditService = auditService;
    }

    @GetMapping("/email-templates")
    public List<TemplateView> listTemplates() {
        return templates.list();
    }

    @GetMapping("/email-templates/{code}")
    public TemplateView getTemplate(@PathVariable String code) {
        return templates.get(code);
    }

    @PutMapping("/email-templates/{code}")
    public TemplateView updateTemplate(@PathVariable String code, @Valid @RequestBody TemplateRequest request) {
        return templates.update(code, request);
    }

    @PostMapping("/email-templates/{code}/reset")
    public TemplateView resetTemplate(@PathVariable String code) {
        return templates.reset(code);
    }

    @PostMapping("/email-templates/{code}/preview")
    public Rendered previewTemplate(@PathVariable String code, @Valid @RequestBody TemplateRequest request) {
        return templates.preview(code, request);
    }

    @PostMapping("/email-templates/{code}/test")
    public TestResult testTemplate(@PathVariable String code) {
        return templates.sendTest(code);
    }

    @GetMapping("/reminders")
    public List<ReminderView> listReminders() {
        return reminders.list();
    }

    @PutMapping("/reminders/{code}")
    public ReminderView updateReminder(@PathVariable String code, @Valid @RequestBody ReminderRequest request) {
        return reminders.update(code, request);
    }

    @GetMapping("/reminders/log")
    public List<LogEntry> log() {
        return reminders.log();
    }

    @PostMapping("/reminders/run")
    public RunResult run() {
        RunResult result = job.run();
        auditService.record(AuditActions.REMINDERS_RUN, AuditActions.ENTITY_REMINDER, "run",
                "Ran reminders manually: " + result.sent(), null);
        return result;
    }
}
