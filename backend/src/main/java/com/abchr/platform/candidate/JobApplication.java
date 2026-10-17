package com.abchr.platform.candidate;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_application")
public class JobApplication extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CandidateSource source;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected JobApplication() {
    }

    public JobApplication(UUID jobId, UUID candidateId, CandidateSource source) {
        this.id = UUID.randomUUID();
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.source = source;
        this.appliedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public CandidateSource getSource() {
        return source;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}
