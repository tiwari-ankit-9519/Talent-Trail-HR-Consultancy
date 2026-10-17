package com.abchr.platform.reminder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReminderDtos {

    private ReminderDtos() {
    }

    public record VariableView(String name, String description, String sample) {
    }

    public record TemplateView(
            String code,
            String name,
            String description,
            String audience,
            List<VariableView> variables,
            String subject,
            String body,
            String defaultSubject,
            String defaultBody,
            boolean customised,
            Instant updatedAt) {
    }

    public record TemplateRequest(
            @NotBlank @Size(max = 200) String subject, @NotBlank @Size(max = 6000) String body) {
    }

    public record Rendered(String subject, String body) {
    }

    public record TestResult(String status, String error, String to) {
    }

    public record ReminderView(
            String code,
            String name,
            String description,
            String audience,
            String reference,
            String offsetsHelp,
            List<String> templates,
            boolean enabled,
            List<Integer> offsets,
            List<String> extraRecipients) {
    }

    public record ReminderRequest(
            boolean enabled,
            @NotNull @Size(min = 1, max = 8) List<Integer> offsets,
            @NotNull @Size(max = 5) List<@Size(max = 255) String> extraRecipients) {
    }

    public record LogEntry(
            UUID id, String code, int offsetDays, String recipients, String subject, Instant sentAt) {
    }

    public record RunResult(Map<String, Integer> sent, boolean mailConfigured) {
    }
}
