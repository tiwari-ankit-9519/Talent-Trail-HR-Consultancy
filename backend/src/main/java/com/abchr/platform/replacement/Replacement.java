package com.abchr.platform.replacement;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "replacement")
public class Replacement extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "placement_id", nullable = false, updatable = false)
    UUID placementId;

    @Column(name = "client_id", nullable = false, updatable = false)
    UUID clientId;

    @Column(name = "job_id", nullable = false, updatable = false)
    UUID jobId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    UUID candidateId;

    @Column(name = "joining_date", nullable = false, updatable = false)
    LocalDate joiningDate;

    @Column(name = "exit_date", nullable = false)
    LocalDate exitDate;

    @Column(name = "exit_reason", nullable = false, length = 500)
    String exitReason;

    @Column(name = "period_days", nullable = false, updatable = false)
    int periodDays;

    @Column(name = "free_until", nullable = false, updatable = false)
    LocalDate freeUntil;

    @Column(name = "within_period", nullable = false)
    boolean withinPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    ReplacementStatus status;

    @Column(name = "replacement_placement_id")
    UUID replacementPlacementId;

    @Column(name = "close_note", length = 500)
    String closeNote;

    protected Replacement() {
    }

    public static Replacement create() {
        return new Replacement();
    }

    public UUID getId() {
        return id;
    }
}
