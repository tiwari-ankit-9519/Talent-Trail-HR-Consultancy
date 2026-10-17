package com.abchr.platform.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_email", updatable = false, length = 255)
    private String actorEmail;

    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private String action;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", updatable = false, length = 100)
    private String entityId;

    @Column(name = "summary", nullable = false, updatable = false, length = 500)
    private String summary;

    @Column(name = "details", updatable = false, columnDefinition = "text")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 10)
    private AuditOutcome outcome;

    @Column(name = "ip_address", updatable = false, length = 64)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, length = 300)
    private String userAgent;

    @Column(name = "request_id", updatable = false, length = 64)
    private String requestId;

    protected AuditLog() {
    }

    public AuditLog(
            UUID actorId,
            String actorEmail,
            String action,
            String entityType,
            String entityId,
            String summary,
            String details,
            AuditOutcome outcome,
            String ipAddress,
            String userAgent,
            String requestId) {
        this.id = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary;
        this.details = details;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestId = requestId;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestId() {
        return requestId;
    }
}
