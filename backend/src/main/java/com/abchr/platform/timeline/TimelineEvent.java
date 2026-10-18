package com.abchr.platform.timeline;

import com.abchr.platform.audit.AuditLog;
import com.abchr.platform.audit.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

public record TimelineEvent(
        UUID id,
        Instant occurredAt,
        String action,
        String entityType,
        String summary,
        String actorEmail,
        AuditOutcome outcome) {

    public static TimelineEvent from(AuditLog log) {
        return new TimelineEvent(
                log.getId(),
                log.getOccurredAt(),
                log.getAction(),
                log.getEntityType(),
                log.getSummary(),
                log.getActorEmail(),
                log.getOutcome());
    }
}
