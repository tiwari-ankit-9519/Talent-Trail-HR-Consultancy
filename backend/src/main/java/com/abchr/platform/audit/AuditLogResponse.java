package com.abchr.platform.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        Instant occurredAt,
        UUID actorId,
        String actorEmail,
        String action,
        String entityType,
        String entityId,
        String summary,
        String details,
        AuditOutcome outcome,
        String ipAddress,
        String requestId) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getActorId(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getSummary(),
                log.getDetails(),
                log.getOutcome(),
                log.getIpAddress(),
                log.getRequestId());
    }
}
