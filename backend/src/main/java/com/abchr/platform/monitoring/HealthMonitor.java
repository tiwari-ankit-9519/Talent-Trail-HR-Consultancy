package com.abchr.platform.monitoring;

import com.abchr.platform.notification.NotificationService;
import com.abchr.platform.shared.mail.MailService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthMonitor {

    private static final Logger LOG = LogManager.getLogger(HealthMonitor.class);

    private final SystemHealthService healthService;
    private final NotificationService notifications;
    private final MailService mailService;
    private final List<String> alertEmails;
    private final Duration reminder;
    private final Map<String, Instant> failingSince = new HashMap<>();
    private final Map<String, Instant> lastAlert = new HashMap<>();

    public HealthMonitor(
            SystemHealthService healthService,
            NotificationService notifications,
            MailService mailService,
            @Value("${app.monitoring.alert-emails:}") List<String> alertEmails,
            @Value("${app.monitoring.reminder-hours:6}") long reminderHours) {
        this.healthService = healthService;
        this.notifications = notifications;
        this.mailService = mailService;
        this.alertEmails = alertEmails.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
        this.reminder = Duration.ofHours(reminderHours);
    }

    @Scheduled(initialDelayString = "60000", fixedDelayString = "${app.monitoring.interval-ms:300000}")
    public void check() {
        HealthReport report;
        try {
            report = healthService.run();
        } catch (RuntimeException ex) {
            LOG.error("Health check run failed", ex);
            return;
        }
        Instant now = Instant.now();
        for (HealthCheck check : report.checks()) {
            if (check.ok()) {
                if (failingSince.remove(check.key()) != null) {
                    lastAlert.remove(check.key());
                    alert("Recovered: " + check.key(), check.key() + " is healthy again. " + check.detail());
                }
                continue;
            }
            Instant since = failingSince.computeIfAbsent(check.key(), key -> now);
            Instant last = lastAlert.get(check.key());
            if (last == null || Duration.between(last, now).compareTo(reminder) >= 0) {
                lastAlert.put(check.key(), now);
                alert("Problem: " + check.key(), check.key() + " has been failing since " + since + ". "
                        + check.detail());
            }
        }
    }

    private void alert(String title, String body) {
        LOG.warn("System alert: {} - {}", title, body);
        try {
            List<UUID> admins = notifications.usersWithAnyPermission("SYSTEM_VIEW");
            notifications.notifyUsers(admins, "system", title, body, "/system");
        } catch (RuntimeException ex) {
            LOG.warn("Could not create in-app alert: {}", ex.toString());
        }
        if (!alertEmails.isEmpty()) {
            try {
                mailService.send(alertEmails, "[Talent Trail] " + title, body, null, null);
            } catch (RuntimeException ex) {
                LOG.warn("Could not send alert email: {}", ex.toString());
            }
        }
    }
}
